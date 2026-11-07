package io.microconfig.core

import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.io.yaml.YamlTree
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit.SECONDS

class ConcurrencyIsolationTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun isolateParallelBuildsWithSharedAliasesProfilesCyclesAndTemplates() {
        val aliases = (0..15).map { "alias$it:original" }
        write("envs/dev.yaml", "profiles: [base]\ngroup:\n  components: [${aliases.joinToString(", ")}]\n")
        write("envs/base.yaml", "{}\n")
        write(
            "components/original/application.yaml", """
            #include shared
            name: ${'$'}{this@name}
            left: ${'$'}{left@value}
            right: ${'$'}{right@value}
            mc.template.t: ${'$'}{this@configRoot}/template.txt -> rendered.txt
            """.trimIndent()
        )
        write(
            "components/shared/application.yaml", """
            items: &items [{port: 80, enabled: true}, 'true', null]
            copy: *items
            calculated: #{6 * 7}
            """.trimIndent()
        )
        write("components/shared/application.base.yaml", "profile: inherited\n")
        write("components/left/application.yaml", "value: \${right@value:left-default}\n")
        write("components/right/application.yaml", "value: \${left@value:right-default}\n")
        write("template.txt", "\${this@name}:\${this@left}/\${this@right}\n")
        val baseline = root.resolve("baseline")
        MicroconfigRunner(root.toFile(), baseline.toFile()).build("dev", emptyList(), emptyList())
        val expected = outputFiles(baseline)
        assertEquals(32, expected.size)

        val pool = ForkJoinPool(4)
        try {
            repeat(4) { round ->
                val ready = CountDownLatch(4)
                val builds = (0..3).map { index ->
                    pool.submit<Map<String, String>> {
                        ready.countDown()
                        assertTrue(ready.await(10, SECONDS))
                        val destination = root.resolve("parallel-$round-$index")
                        MicroconfigRunner(root.toFile(), destination.toFile())
                            .build("dev", emptyList(), emptyList())
                        outputFiles(destination)
                    }
                }
                builds.forEach { assertEquals(expected, it.get(30, SECONDS)) }
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun keepYamlAnchorsTagsAndMergeStateSeparateWhenSharingATreeWriter() {
        val tree = YamlTree.standard()
        val pool = ForkJoinPool(4)
        try {
            pool.submit {
                (0..255).toList().parallelStream().forEach { index ->
                    val yaml = tree.toYaml(
                        linkedMapOf(
                            "items" to "[&item {id: $index, enabled: true}, *item]",
                            "merged" to "{base: &base {id: $index}, copy: {<<: *base, extra: 'true'}}",
                            "typed" to "!!str true",
                            "literal" to "|-\n  line-$index\n  second\n",
                        )
                    )
                    val result = Yaml().load<Map<String, Any?>>(yaml)
                    val item = mapOf("id" to index, "enabled" to true)
                    assertEquals(listOf(item, item), result["items"])
                    assertEquals(
                        mapOf(
                            "base" to mapOf("id" to index),
                            "copy" to mapOf("id" to index, "extra" to "true")
                        ), result["merged"]
                    )
                    assertEquals("true", result["typed"])
                    assertEquals("line-$index\nsecond", result["literal"])
                }
            }.get(30, SECONDS)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun shareExpressionFunctionsAndConversionsWithoutLeakingFailedAssignments() {
        val resolver = ExpressionResolver()
        val context = DeclaringComponentImpl("app", "component", "dev")
        val cases = listOf(
            "#{#base64('hello')}" to "aGVsbG8=",
            "#{'hello'.toUpperCase()}" to "HELLO",
            "#{'hello'.bytes.length}" to "5",
            "#{ {'a','b'} }" to "a,b",
            "#{#findGroup('([a-z]+)', '123value')}" to "value",
            "#{true}" to "true",
            "#{6 * 7}" to "42",
        )
        val pool = ForkJoinPool(4)
        try {
            pool.submit {
                (0..255).toList().parallelStream().forEach { index ->
                    val mutation = if (index % 2 == 0) "#{#base64 = 1}" else "#{#base64++}"
                    assertThrows(RuntimeException::class.java) { resolver.resolve(mutation, context, context) }
                    val (expression, expected) = cases[index % cases.size]
                    assertEquals(expected, resolver.resolve(expression, context, context))
                    assertEquals("aGVsbG8=", resolver.resolve("#{#base64('hello')}", context, context))
                }
            }.get(30, SECONDS)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun outputFiles(directory: Path): Map<String, String> =
        Files.walk(directory).use { paths ->
            paths.filter(Files::isRegularFile).toList().associate {
                directory.relativize(it).toString() to Files.readString(it)
            }
        }
}
