package io.microconfig.core

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit.SECONDS

class ParallelBuildTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun keepDynamicDefaultsConsistentAndIsolatedInParallelStreams() {
        val names = (0..31).map { "service$it" }
        write("envs/dev.yaml", "group:\n  components: [${names.joinToString(", ") { "$it:original" }}]\n")
        write(
            "components/original/application.yaml", """
            gate: ${'$'}{gate@value}
            stamp: ${'$'}{this@missing:#{#currentTimeMillis()}}-${'$'}{this@name}
            items: ${'$'}{this@missingItems:[${'$'}{this@stamp}, ${'$'}{this@stamp}]}
            """.trimIndent() + "\n" +
                    (0..7).joinToString("") { "ref$it: \${this@stamp}\n" })
        val threads = ConcurrentHashMap.newKeySet<Thread>()
        val arrived = CountDownLatch(2)
        val strategy = PlaceholderResolveStrategy { component, key, _, _, _ ->
            if (component != "gate") Optional.empty()
            else {
                threads.add(Thread.currentThread())
                arrived.countDown()
                check(arrived.await(5, SECONDS)) { "Expected parallel component resolution" }
                Optional.of(PropertyImpl.property(key, "ready", ConfigFormat.PROPERTIES, null))
            }
        }
        val configured = Microconfig.searchConfigsIn(root.toFile()).withAdditionalPlaceholderResolvers(listOf(strategy))
        val pool = ForkJoinPool(4)
        try {
            val built = pool.submit<io.microconfig.core.properties.Properties> {
                MicroconfigRunner(configured).buildProperties("dev", emptyList(), emptyList())
            }.get(20, SECONDS)
            val applications = built.asTypedProperties().filter { it.configType.typeName == "app" }

            assertTrue(threads.size > 1)
            assertEquals(names, applications.map { it.declaringComponent.component })
            applications.forEach { properties ->
                val values = properties.propertiesAsKeyValue
                val stamp = Yaml().load<String>(values.getValue("stamp"))
                assertTrue(stamp.endsWith("-${properties.declaringComponent.component}"), stamp)
                (0..7).forEach { assertEquals(stamp, Yaml().load<String>(values.getValue("ref$it"))) }
                assertEquals(listOf(stamp, stamp), Yaml().load<List<String>>(values.getValue("items")))
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun preserveSharedIncludesProfilesAliasesCrossEnvironmentReferencesAndRebuildDiffs() {
        val names = (0..15).map { "service$it" }
        write(
            "envs/dev.yaml", "profiles: [base]\ngroup:\n  components: [" +
                names.mapIndexed { index, name -> "$name:original$index" }.joinToString(", ") + "]\n"
        )
        write("envs/base.yaml", "{}\n")
        write("envs/prod.yaml", "{}\n")
        write(
            "components/shared/application.yaml", """
            shared: ${'$'}{this@local}
            items: &items [true, 'true', {port: 80}]
            alias: *items
            """.trimIndent()
        )
        write("components/shared/application.base.yaml", "profile: inherited\n")
        write("components/source/application.prod.yaml", "value: production\n")
        for (index in names.indices) write(
            "components/original$index/application.yaml", """
            #include shared
            name: ${'$'}{this@name}
            local: root-$index
            copy: ${'$'}{this@shared}
            explicit: ${'$'}{source[prod]@value}
            calculated: #{6 * 7}
            """.trimIndent()
        )
        val reads = ConcurrentHashMap<File, Int>()
        val fallback = DumpedFsReader()
        val reader = object : FsReader by fallback {
            override fun readFully(file: File): String {
                reads.merge(file.canonicalFile, 1, Int::plus)
                return fallback.readFully(file)
            }
        }
        val destination = root.resolve("output")
        val pool = ForkJoinPool(4)
        try {
            repeat(2) { round ->
                val configured = Microconfig.searchConfigsIn(root.toFile()).withDestinationDir(destination.toFile())
                val microconfig = if (round == 0) configured else configured.withFsReader(reader)
                val runner = MicroconfigRunner(microconfig)
                pool.submit { runner.build("dev", emptyList(), emptyList()) }.get(20, SECONDS)

                names.forEachIndexed { index, name ->
                    val output = destination.resolve("$name/application.yaml")
                    val values = Yaml().load<Map<String, Any?>>(Files.readString(output))
                    assertEquals(name, values["name"])
                    assertEquals("root-$index", values["shared"])
                    assertEquals("root-$index", values["copy"])
                    assertEquals("production", values["explicit"])
                    assertEquals("inherited", values["profile"])
                    assertEquals(42, values["calculated"])
                    assertEquals(listOf(true, "true", mapOf("port" to 80)), values["items"])
                    assertEquals(values["items"], values["alias"])
                    assertFalse(Files.exists(destination.resolve("$name/diff-application.yaml")))
                }
                if (round == 1) assertEquals(
                    1,
                    reads[root.resolve("components/shared/application.yaml").toFile().canonicalFile]
                )
            }
            write("components/source/application.prod.yaml", "value: changed\n")
            pool.submit {
                MicroconfigRunner(root.toFile(), destination.toFile())
                    .build("dev", emptyList(), emptyList())
            }.get(20, SECONDS)
            for (name in names) {
                val diff = Yaml().load<Map<String, Any?>>(
                    Files.readString(
                        destination.resolve("$name/diff-application.yaml")
                    )
                )
                assertEquals(mapOf("explicit" to "production -> changed"), diff)
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["reader", "repository", "strategy"])
    fun supportStatelessExtensionsDuringParallelBuilds(extension: String) {
        val names = (0..15).map { "component$it" }
        write("envs/dev.yaml", "group:\n  components: [${names.joinToString(", ")}]\n")
        names.forEach { write("components/$it/application.yaml", "value: \${this@name}\n") }
        val visited = ConcurrentLinkedQueue<String>()
        val threads = ConcurrentHashMap.newKeySet<Thread>()
        val arrived = CountDownLatch(2)
        fun visit(name: String) {
            visited.add(name)
            threads.add(Thread.currentThread())
            arrived.countDown()
            assertTrue(arrived.await(5, SECONDS), "Extensions must run in parallel too")
        }

        val base = Microconfig.searchConfigsIn(root.toFile())
        val microconfig = when (extension) {
            "reader" -> {
                val fs = DumpedFsReader()
                base.withFsReader(object : FsReader by fs {
                    override fun readFully(file: File): String {
                        if (file.parentFile.name in names) visit(file.parentFile.name)
                        return fs.readFully(file)
                    }
                })
            }

            "repository" -> base.withAdditionalPropertiesRepositories(listOf(PropertiesRepository { name, _, type ->
                if (type.typeName == "app") visit(name)
                emptyMap()
            }))

            else -> base.withAdditionalPlaceholderResolvers(listOf(PlaceholderResolveStrategy { name, key, _, _, _ ->
                if (key == "name") visit(name)
                Optional.empty()
            }))
        }

        val pool = ForkJoinPool(4)
        try {
            val properties = pool.submit<io.microconfig.core.properties.Properties> {
                MicroconfigRunner(microconfig).buildProperties("dev", emptyList(), emptyList())
            }.get(20, SECONDS)
            assertEquals(names.sorted(), visited.sorted())
            assertTrue(threads.size > 1)
            assertEquals(names, properties.asTypedProperties().filter { it.configType.typeName == "app" }
                .map { it.properties.single().value })
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun reportTheFirstComponentFailureWithoutWorkerExceptionWrapping() {
        val names = listOf("first") + (1..15).map { "component$it" } + "last"
        write("envs/dev.yaml", "group:\n  components: [${names.joinToString(", ")}]\n")
        names.forEach { write("components/$it/application.yaml", "value: valid\n") }
        write("components/first/application.yaml", "value: [unterminated\n")
        write("components/last/application.yaml", "value: #{1 / 0}\n")

        val failure = assertThrows(IllegalArgumentException::class.java) {
            MicroconfigRunner(root.toFile(), root.resolve("output").toFile())
                .buildProperties("dev", emptyList(), emptyList())
        }

        assertTrue(failure.message!!.startsWith("Can't parse YAML"), failure.message)
        assertTrue(failure.message!!.contains("first"), failure.message)
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }
}
