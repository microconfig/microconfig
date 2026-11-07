package io.microconfig.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MustachePartialsTest {
    @TempDir lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest
    @CsvSource("mc.mustache,txt", "mc.template,mustache")
    fun resolveMicroconfigPlaceholdersInNestedPartials(prefix: String, extension: String) {
        write(
            "components/app/application.yaml",
            "value: world\n$prefix.t: \${this@configDir}/main.$extension -> rendered.txt\n",
        )
        write("components/app/main.$extension", "hello {{>partial.txt}}")
        write(
            "components/app/partial.txt",
            "\${this@value}|\${value}|\${this@name}|\${this@templateName}|" +
                "\${this@missing:fallback}|\\\${this@value}|{{value}}|{{>nested.txt}}|\${UNKNOWN}",
        )
        write("components/app/nested.txt", "\${this@value}")

        build()

        assertEquals(
            "hello world|world|app|t|fallback|\${this@value}|world|world|\${UNKNOWN}",
            Files.readString(destination.resolve("app/rendered.txt")),
        )
    }

    @Test
    fun reportMissingRequiredPlaceholderInPartialBeforeWriting() {
        write(
            "components/app/application.yaml",
            "value: world\nmc.mustache.t: \${this@configDir}/main.txt -> rendered.txt\n",
        )
        write("components/app/main.txt", "{{>partial.txt}}")
        write("components/app/partial.txt", "\${this@missing}")

        assertThrows(IllegalStateException::class.java) { build() }

        assertFalse(Files.exists(destination.resolve("app/rendered.txt")))
        assertFalse(Files.exists(destination.resolve("app/application.yaml")))
    }

    private fun build() =
        MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", emptyList(), emptyList())

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }
}
