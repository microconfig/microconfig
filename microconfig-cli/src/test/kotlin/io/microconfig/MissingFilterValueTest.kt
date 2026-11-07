package io.microconfig

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class MissingFilterValueTest {
    @TempDir lateinit var directory: Path
    private val root get() = directory.resolve("repo")
    private val destination get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        write(root.resolve("envs/dev.yaml"), "group:\n  components: [first, second]\n")
        for (name in listOf("first", "second")) {
            write(root.resolve("components/$name/application.yaml"), "value: new-$name\n")
            write(destination.resolve("$name/application.yaml"), "value: old-$name\n")
            write(destination.resolve("$name/diff-application.yaml"), "retained-$name")
        }
    }

    @ParameterizedTest
    @MethodSource("emptyFilters")
    fun rejectEmptyFiltersBeforeTouchingAnyOutput(option: String, value: String?) {
        val arguments = listOf("-r", root.toString(), "-d", destination.toString(), "-e", "dev",
            "-color", "false", option) + listOfNotNull(value)
        val process = cli(arguments)
        try {
            assertTrue(process.waitFor(10, SECONDS), "CLI did not reject an empty filter")
            val text = process.inputStream.bufferedReader().use { it.readText() }

            assertNotEquals(0, process.exitValue(), text)
            assertTrue(text.contains("$option requires at least one name"), text)
            assertFalse(text.contains("\u001B["), text)
            assertFalse(text.contains("Exception in thread"), text)
            for (name in listOf("first", "second")) {
                assertEquals("value: old-$name\n", Files.readString(destination.resolve("$name/application.yaml")))
                assertEquals("retained-$name", Files.readString(destination.resolve("$name/diff-application.yaml")))
            }
        } finally {
            process.destroyForcibly()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["-s", "-g"])
    fun retainValidFilters(option: String) {
        val selected = if (option == "-s") "first" else "group"
        val process = cli(listOf("-r", root.toString(), "-d", destination.toString(), "-e", "dev",
            "-color", "false", option, selected))
        try {
            assertTrue(process.waitFor(10, SECONDS), "CLI did not finish")
            val text = process.inputStream.bufferedReader().use { it.readText() }

            assertEquals(0, process.exitValue(), text)
            assertEquals("value: new-first\n", Files.readString(destination.resolve("first/application.yaml")))
            val second = if (option == "-s") "old-second" else "new-second"
            assertEquals("value: $second\n", Files.readString(destination.resolve("second/application.yaml")))
        } finally {
            process.destroyForcibly()
        }
    }

    private fun cli(arguments: List<String>): Process =
        ProcessBuilder(listOf(File(System.getProperty("java.home"), "bin/java").path,
            "-jar", requireNotNull(System.getProperty("microconfig.cli.jar"))) + arguments)
            .redirectErrorStream(true).start()

    private fun write(file: Path, text: String) {
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    companion object {
        @JvmStatic
        fun emptyFilters(): List<Arguments> = listOf("-s", "-g").flatMap { option ->
            listOf(null, "", "   ", ", ,", "\t\r\n", "\"\"").map { Arguments.of(option, it) }
        }
    }
}
