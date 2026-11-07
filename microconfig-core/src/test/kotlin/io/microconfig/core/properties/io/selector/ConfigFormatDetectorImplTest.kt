package io.microconfig.core.properties.io.selector

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.io.impl.DumpedFsReader
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ConfigFormatDetectorImplTest {
    @TempDir lateinit var directory: Path
    private val io = ConfigIoFactory.newConfigIo(DumpedFsReader())

    @ParameterizedTest
    @ValueSource(strings = ["process", "proc", "custom"])
    fun skipPropertiesBangCommentsContainingSeparators(extension: String) {
        val file = directory.resolve("config.$extension")
        Files.writeString(
            file,
            "\n# header: ignored\n.var ignored: yaml\n" +
                "  ! JVM settings: used by launcher\n! options = ignored\n!\tMore: ignored\n" +
                "!withoutSeparator\nheap=256m\n",
        )

        val reader = io.readFrom(file.toFile())

        assertEquals(PROPERTIES, reader.format())
        assertEquals(mapOf("heap" to "256m"), reader.propertiesAsMap())
    }

    @Test
    fun skipUnspacedPropertiesBangCommentsContainingSeparators() {
        val file = directory.resolve("config.process")
        Files.writeString(file, "!JVM settings: used by launcher\nheap=256m\n")

        val reader = io.readFrom(file.toFile())

        assertEquals(PROPERTIES, reader.format())
        assertEquals(mapOf("heap" to "256m"), reader.propertiesAsMap())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "!!str key: retained\n",
            "!custom key: retained\n",
            "! key: retained\n",
            "! key: |-\n  a=b\n",
            "!<tag:example.com,2026:key> key: retained\n",
            "!!map\nkey: retained\n",
            "!custom key: |-\n  a=b\n",
            "%YAML 1.1\n---\nkey: retained\n",
            "%TAG !e! tag:yaml.org,2002:\n---\n!e!map\nkey: retained\n",
            "'with=equals': retained\n",
            "\"with=equals\": retained\n",
            "parent.'with=equals': retained\n",
        ]
    )
    fun retainYamlDetectionForTagsHeadersAndQuotedKeys(content: String) {
        val file = directory.resolve("config.process")
        Files.writeString(file, content)

        val reader = io.readFrom(file.toFile())

        assertEquals(YAML, reader.format())
        assertEquals(1, reader.propertiesAsMap().size)
    }

    @Test
    fun retainNonspecificYamlTagsWithMicroconfigStatements() {
        val file = directory.resolve("config.process")
        val value = "\${source@value:{nested: fallback}}"
        Files.writeString(file, "! key: $value\n")

        val reader = io.readFrom(file.toFile())

        assertEquals(YAML, reader.format())
        assertEquals(value, reader.properties("", "").single().value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["!Custom {key: value}\n", "!!set {key: null}\n"])
    fun retainErrorsForUnsupportedYamlRootTags(content: String) {
        val file = directory.resolve("config.process")
        Files.writeString(file, content)

        val reader = io.readFrom(file.toFile())

        assertEquals(YAML, reader.format())
        val error = assertThrows(IllegalArgumentException::class.java) {
            reader.properties("", "")
        }
        assertTrue(error.message.orEmpty().contains("YAML configuration must contain a mapping"))
    }

    @Test
    fun retainEscapedSeparatorsInPropertiesKeys() {
        val file = directory.resolve("config.process")
        Files.writeString(file, "key\\:part=value\n")

        val reader = io.readFrom(file.toFile())

        assertEquals(PROPERTIES, reader.format())
        assertEquals(mapOf("key\\:part" to "value"), reader.propertiesAsMap())
    }

    @Test
    fun retainNonspecificYamlTagsWithExplicitExtension() {
        val file = directory.resolve("config.yaml")
        Files.writeString(file, "! key: retained\n")

        val reader = io.readFrom(file.toFile())

        assertEquals(YAML, reader.format())
        assertEquals(mapOf("key" to "retained"), reader.propertiesAsMap())
    }
}
