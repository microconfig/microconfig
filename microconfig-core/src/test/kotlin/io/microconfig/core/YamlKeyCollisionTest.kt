package io.microconfig.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path

class YamlKeyCollisionTest {
    @TempDir
    lateinit var root: Path

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun rejectInheritedDescendantOverwritingLiteralKeyBeforeWriting(reverse: Boolean, section: Boolean) {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/base/application.yaml", "a:\n  b: nested\n")
        val literal = if (section) "'a.b':\n  child: literal" else "'a.b': literal"
        val localProperties = listOf("a: root", literal)
        val declarations = if (reverse) localProperties.reversed() else localProperties
        write("components/app/application.yaml", "#include base\n" + declarations.joinToString("\n"))
        write("output/app/application.yaml", "retained: true\n")
        val runner = MicroconfigRunner(root.toFile(), root.resolve("output").toFile())

        val error = assertThrows(IllegalArgumentException::class.java) {
            runner.build("dev", emptyList(), emptyList())
        }

        assertTrue(error.message.orEmpty().contains("same YAML key"), error.message)
        assertEquals("retained: true\n", Files.readString(root.resolve("output/app/application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun rejectLiteralKeyOverwritingPreviouslyCollapsedInheritedPath(section: Boolean) {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        val descendant = if (section) "  c:\n    d: nested\n" else "  c: nested\n"
        write("components/base/application.yaml", "'a.b':\n$descendant")
        write("components/app/application.yaml", "#include base\n'a.b': root\n'a.b.c': literal\n")
        write("output/app/application.yaml", "retained: true\n")
        val runner = MicroconfigRunner(root.toFile(), root.resolve("output").toFile())

        val error = assertThrows(IllegalArgumentException::class.java) {
            runner.build("dev", emptyList(), emptyList())
        }

        assertTrue(error.message.orEmpty().contains("same YAML key"), error.message)
        assertEquals("retained: true\n", Files.readString(root.resolve("output/app/application.yaml")))
    }

    private fun write(name: String, content: String) {
        val file = root.resolve(name)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }
}
