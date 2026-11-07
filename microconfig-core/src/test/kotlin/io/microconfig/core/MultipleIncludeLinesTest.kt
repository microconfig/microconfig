package io.microconfig.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class MultipleIncludeLinesTest {
    @TempDir
    lateinit var root: Path

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun mergeIncludesDeclaredOnSeparateLines(format: String) {
        createRepository(format)

        build()

        assertEquals(
            mapOf(
                "firstOnly" to "first",
                "secondOnly" to "second",
                "shared" to "second",
                "localOnly" to "app",
            ),
            readOutput(format),
        )
    }

    private fun createRepository(format: String) {
        val separator = if (format == "yaml") ": " else "="
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write(
            "components/first/application.$format",
            "firstOnly${separator}first\nshared${separator}first\n",
        )
        write(
            "components/second/application.$format",
            "secondOnly${separator}second\nshared${separator}second\n",
        )
        write(
            "components/app/application.$format",
            "#include first\n#include second\nlocalOnly${separator}app\n",
        )
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build() =
        MicroconfigRunner(root.toFile(), root.resolve("output").toFile())
            .build("dev", emptyList(), emptyList())

    private fun readOutput(format: String): Map<*, *> {
        val text = Files.readString(root.resolve("output/app/application.$format"))
        return if (format == "yaml") Yaml().load<Map<*, *>>(text)
        else Properties().apply { load(StringReader(text)) }
    }
}
