package io.microconfig.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.Yaml

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListDefaultValuesTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/source/application.yaml",
            "items: [one, two]\nempty: []\nscalar: 'a: b'\nnumber: '123'\nflag: true\n")
    }

    @ParameterizedTest(name = "whole: {0}")
    @MethodSource("listDefaults")
    fun resolveWholeListDefault(expression: String, expected: List<*>) {
        write("components/app/application.yaml", "result: $expression\nsibling: tail\n")
        val result = build()
        assertEquals(expected, result["result"])
        assertEquals("tail", result["sibling"])
    }

    @ParameterizedTest(name = "nested and merged: {0}")
    @MethodSource("listDefaults")
    fun resolveNestedAndMergedListDefault(expression: String, expected: List<*>) {
        write("components/app/application.yaml",
            "nested: [$expression]\nmerged:\n  - before\n  $expression\n  - after\nsibling: tail\n")
        val result = build()
        assertEquals(listOf(expected), result["nested"])
        assertEquals(listOf("before") + expected + "after", result["merged"])
        assertEquals("tail", result["sibling"])
    }

    @ParameterizedTest(name = "component={0}, reverse={1}")
    @MethodSource("listReferences")
    fun resolveReferencesToListWhoseElementsContainPlaceholders(component: String, reverse: Boolean) {
        write("components/common/application.yaml", "db:\n  host: localhost\n  port: 5432\n  auth:\n    user: reader\n")
        val declarations = """
            items:
              - ${'$'}{common@db.host}
              - "host=${'$'}{common@db.host}"
              - database: ${'$'}{common@db.*}
              - ${'$'}{missing@value:fallback}
              - #{1 + 2}
              - ${'$'}{missing@items:[]}
              - ${'$'}{missing@items:[#{2 * 3}, ${'$'}{common@db.*}]}
            empty: []
        """.trimIndent() + "\n"
        val references = listOf(
            "copy: \${$component@items}\n", "chain: \${this@copy}\n",
            "wrapped: [\${this@copy}]\n", "emptyCopy: \${$component@empty}\n",
            "merged:\n  - before\n  \${this@copy}\n  - after\n",
        ).let { if (reverse) it.reversed() else it }.joinToString("")
        if (component == "source") write("components/source/application.yaml", declarations)
        write("components/app/application.yaml", if (component == "source") references
            else if (reverse) declarations + references else references + declarations)
        val result = build()
        val group = mapOf("host" to "localhost", "port" to 5432, "auth" to mapOf("user" to "reader"))
        val expected = listOf("localhost", "host=localhost", mapOf("database" to group),
            "fallback", 3, emptyList<Any>(), listOf(6, group))
        assertEquals(expected, result["copy"])
        assertEquals(expected, result["chain"])
        assertEquals(listOf(expected), result["wrapped"])
        assertEquals(listOf("before") + expected + "after", result["merged"])
        assertEquals(emptyList<Any>(), result["emptyCopy"])
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun build(): Map<String, Any?> {
        val output = root.resolve("output")
        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())
        return Yaml().load(Files.readString(output.resolve("app/application.yaml")))
    }

    private fun listDefaults(): List<Arguments> {
        val items = listOf("one", "two")
        return listOf(
            Arguments.of("\${missing@items:[]}", emptyList<Any>()),
            Arguments.of("\${missing@items:\${source@empty}}", emptyList<Any>()),
            Arguments.of("\${missing@items:\${missing@other:\${source@empty}}}", emptyList<Any>()),
            Arguments.of("\${missing@items:\${source@items}}", items),
            Arguments.of("\${missing@items:[\${source@items}]}", listOf(items)),
            Arguments.of("\${missing@items:[before, \${source@empty}, after]}",
                listOf("before", emptyList<Any>(), "after")),
            Arguments.of("\${missing@items:[#{1 + 2}, #{true}, #{'a, b'}, #{'a}b'}]}",
                listOf(3, true, "a, b", "a}b")),
            Arguments.of("\${missing@items:[\${source@items}, #{2 * 3}, \${source@empty}]}",
                listOf(items, 6, emptyList<Any>())),
            Arguments.of("\${missing@items:[\${source@scalar}, \${source@number}, \${source@flag}]}",
                listOf("a: b", "123", true)),
            Arguments.of("\${missing@items:[\${missing@nested:[#{1 + 2}, \${source@items}]}]}",
                listOf(listOf(3, items))),
            Arguments.of("\${missing@items:[{value: #{1 + 2}, text: \${source@scalar}}, [#{true}, \${source@empty}]]}",
                listOf(mapOf("value" to 3, "text" to "a: b"), listOf(true, emptyList<Any>()))),
            Arguments.of("\${source@empty:[#{1 / 0}]}", emptyList<Any>()),
            Arguments.of("\${source@items:[#{1 / 0}]}", items),
            Arguments.of("\${missing@items:[#{#findGroup('[a-z]{2,3}', '123abc')}]}", listOf("abc")),
            Arguments.of("\${missing@items:[#{#{1 + 2} * 2}]}", listOf(6)),
        )
    }

    private fun listReferences(): List<Arguments> = listOf("this", "source").flatMap { component ->
        listOf(false, true).map { reverse -> Arguments.of(component, reverse) }
    }
}
