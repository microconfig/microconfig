package io.microconfig.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class UnicodeIncludesTest {
    @TempDir
    lateinit var root: Path

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun includeAndReferenceUnicodeComponent(format: String) {
        val component = "общие-настройки_1.v2"
        val separator = if (format == "yaml") ": " else "="
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/$component/application.$format", "value${separator}inherited\n")
        write(
            "components/app/application.$format",
            "#include $component\nreference${separator}\${$component@value}\n",
        )

        val destination = root.resolve("output")
        MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", emptyList(), emptyList())

        val text = Files.readString(destination.resolve("app/application.$format"))
        val result =
            if (format == "yaml") Yaml().load<Map<*, *>>(text)
            else Properties().apply { load(StringReader(text)) }
        assertEquals(mapOf("value" to "inherited", "reference" to "inherited"), result)
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }
}
