package io.microconfig

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.jar.JarFile

class ShadowJarTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun keepFeaturesRegisteredInNativeImageMetadata() {
        JarFile(requireNotNull(System.getProperty("microconfig.cli.jar"))).use { jar ->
            nativeImageFeatures(jar).forEach { feature ->
                assertNotNull(jar.getJarEntry(feature.replace('.', '/') + ".class"), feature)
            }
        }
    }

    private fun nativeImageFeatures(jar: JarFile): List<String> =
        jar.entries().asSequence()
            .filter { it.name.startsWith("META-INF/native-image/") && it.name.endsWith("native-image.properties") }
            .flatMap { entry ->
                val properties = Properties()
                jar.getInputStream(entry).use(properties::load)
                properties.getProperty("Args", "").split(Regex("\\s+"))
                    .filter { it.startsWith("--features=") }
                    .flatMap { it.substringAfter('=').split(',') }
            }.toList()

    @Test
    fun buildPropertiesYamlAndMustacheWithPackagedDependencies() {
        createRepository()
        buildWithPackagedCli()
        assertGeneratedProperties()
        assertEquals(
            "hello|aGVsbG8=|one,two,",
            File(directory, "output/app/rendered.txt").readText(),
        )
        assertTrue(File(directory, "output/app/application.yaml").readText().contains("title: hello"))
    }

    private fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app, props]\n")
        write(
            "components/props/application.properties",
            """
            encoded=#{#base64('hello')}
            encodedBytes=#{#bytesBase64({104, 101, 108, 108, 111})}
            group=#{#findGroup('([0-9]+)', 'a12')}
            fallback=#{#findGroupOrDefault('absent', 'abc', 'fallback')}
            deleted=#{#delete('a-b-c', '-')}
            afterFirst=#{#substringAfterFirst('a-b-c', '-')}
            afterLast=#{#substringAfterLast('a-b-c', '-')}
            removed=#{#removeLineSeparators('a' + #lineSeparator() + 'b')}
            replaced=#{#replaceLineSeparators('a' + #lineSeparator() + 'b', '|')}
            time=#{#currentTimeMillis() > 0}
            collection=#{ {'a', 'b'} }
            method=#{'a,b'.split(',')[1]}
            """.trimIndent() + "\n",
        )
        write(
            "components/app/application.yaml",
            """
            title: hello
            encoded: ${'$'}{props@encoded}
            items: [one, two]
            mc.mustache.report: ${'$'}{this@configDir}/report.mustache -> rendered.txt
            """.trimIndent() + "\n",
        )
        write("components/app/report.mustache", "{{title}}|{{{encoded}}}|{{>items.mustache}}")
        write("components/app/items.mustache", "{{#items}}{{.}},{{/items}}")
    }

    private fun write(path: String, content: String) {
        val file = File(directory, "repo/$path")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun buildWithPackagedCli() {
        val log = File(directory, "cli.log")
        val process = ProcessBuilder(
            File(System.getProperty("java.home"), "bin/java").path,
            "-jar", requireNotNull(System.getProperty("microconfig.cli.jar")),
            "-r", File(directory, "repo").path,
            "-e", "dev",
            "-d", File(directory, "output").path,
            "-color", "false",
            "-stacktrace",
        ).redirectErrorStream(true).redirectOutput(log).start()
        try {
            assertTrue(process.waitFor(30, SECONDS), "Packaged CLI did not finish")
            assertEquals(0, process.exitValue(), log.readText())
        } finally {
            process.destroyForcibly()
        }
    }

    private fun assertGeneratedProperties() {
        val properties = Properties()
        File(directory, "output/props/application.properties").reader().use(properties::load)
        assertEquals(
            mapOf(
                "encoded" to "aGVsbG8=",
                "encodedBytes" to "aGVsbG8=",
                "group" to "12",
                "fallback" to "fallback",
                "deleted" to "abc",
                "afterFirst" to "b-c",
                "afterLast" to "c",
                "removed" to "ab",
                "replaced" to "a|b",
                "time" to "true",
                "collection" to "a,b",
                "method" to "b",
            ),
            properties.entries.associate { it.key.toString() to it.value.toString() },
        )
    }
}
