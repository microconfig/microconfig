package io.microconfig.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path

class OutputCollisionTest {
    @TempDir
    lateinit var root: Path
    private val destination get() = root.resolve("output")

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/app/first.txt", "first")
        write("components/app/second.txt", "second")
        write("output/app/application.yaml", "value: previous\n")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Shared.txt", "nested/Shared.txt", "Shared/Item.txt"])
    fun rejectCaseVariantTemplatesBeforeWriting(first: String) {
        assumeTrue(caseInsensitive())
        templates(first, first.lowercase())

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertPreviousConfig()
        assertFalse(Files.exists(destination.resolve("app/$first")))
    }

    @Test
    fun rejectCaseVariantOfGeneratedConfigBeforeWriting() {
        assumeTrue(caseInsensitive())
        Files.delete(destination.resolve("app/application.yaml"))
        templates("Application.yaml", "other.txt")

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertFalse(Files.exists(destination.resolve("app/Application.yaml")))
        assertFalse(Files.exists(destination.resolve("app/other.txt")))
    }

    @Test
    fun rejectCaseVariantTemplatesInEmptyOutputDirectoryBeforeWriting() {
        assumeTrue(caseInsensitive())
        Files.delete(destination.resolve("app/application.yaml"))
        Files.delete(destination.resolve("app"))
        templates("A.yaml", "a.yaml")

        assertThrows(IllegalArgumentException::class.java) { build() }

        Files.list(destination).use { assertEquals(emptyList<Path>(), it.toList()) }
    }

    @Test
    fun keepCaseDistinctTemplatesOnCaseSensitiveFileSystems() {
        assumeFalse(caseInsensitive())
        templates("Shared.txt", "shared.txt")

        build()

        assertEquals("first", Files.readString(destination.resolve("app/Shared.txt")))
        assertEquals("second", Files.readString(destination.resolve("app/shared.txt")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["DIFF-application.yaml", "diff-application.YAML"])
    fun rejectCaseVariantsOfReservedDiffNames(name: String) {
        assumeTrue(caseInsensitive())
        templates(name, "other.txt")

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertPreviousConfig()
        assertFalse(Files.exists(destination.resolve("app/other.txt")))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun rejectFileAndDirectoryConflictsInEitherDeclarationOrder(reverse: Boolean) {
        val names = listOf("shared", "shared/child.txt").let { if (reverse) it.reversed() else it }
        templates(names[0], names[1])

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertPreviousConfig()
        assertFalse(Files.exists(destination.resolve("app/shared")))
    }

    @Test
    fun preserveExistingFileWhenAnotherOutputUsesItAsADirectory() {
        write("output/app/shared", "retained")
        templates("shared", "shared/child.txt")

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertEquals("retained", Files.readString(destination.resolve("app/shared")))
        assertPreviousConfig()
    }

    @Test
    fun rejectTemplateNestedUnderGeneratedConfig() {
        templates("application.yaml/child.txt", "other.txt")

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertPreviousConfig()
        assertFalse(Files.exists(destination.resolve("app/other.txt")))
    }

    @Test
    fun rejectCaseVariantFileAndDirectoryConflict() {
        assumeTrue(caseInsensitive())
        templates("Shared", "shared/child.txt")

        assertThrows(IllegalArgumentException::class.java) { build() }

        assertPreviousConfig()
        assertFalse(Files.exists(destination.resolve("app/shared")))
    }

    private fun caseInsensitive(): Boolean {
        val probe = root.resolve("case-probe")
        Files.writeString(probe, "probe")
        return Files.exists(root.resolve("CASE-PROBE"))
    }

    private fun templates(first: String, second: String) =
        write(
            "components/app/application.yaml", "value: current\n" +
                    "mc.file.one: \${this@configDir}/first.txt -> $first\n" +
                    "mc.file.two: \${this@configDir}/second.txt -> $second\n"
        )

    private fun write(name: String, value: String) {
        val file = root.resolve(name)
        Files.createDirectories(file.parent)
        Files.writeString(file, value)
    }

    private fun build() = MicroconfigRunner(root.toFile(), destination.toFile())
        .build("dev", emptyList(), emptyList())

    private fun assertPreviousConfig() =
        assertEquals("value: previous\n", Files.readString(destination.resolve("app/application.yaml")))
}
