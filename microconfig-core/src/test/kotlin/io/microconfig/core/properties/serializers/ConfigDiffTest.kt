package io.microconfig.core.properties.serializers

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.io.properties.PropertiesText.escape
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.serializers.impl.ConfigDiff
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class ConfigDiffTest {
    @TempDir lateinit var dir: Path

    @Test
    fun noDiffAfterRebuildWithTheSameYamlValues() {
        val file =
            build(
                "application.yaml",
                listOf(
                    prop("multiline", "first${LINES_SEPARATOR}second", YAML),
                    prop("indented", "  a${LINES_SEPARATOR}b", YAML),
                    prop("backslashes", "C:\\temp\\new", YAML),
                    prop("shellContinuation", "docker run \\$LINES_SEPARATOR  --rm img", YAML),
                    prop("simple", "x", YAML),
                ),
            )
        assertFalse(diffFile(file).exists())
    }

    @Test
    fun noDiffAfterRebuildWithTheSamePropertiesValues() {
        val file =
            build(
                "application.properties",
                listOf(
                    prop("multiline", "first${LINES_SEPARATOR}second", PROPERTIES),
                    prop("indented", "  a", PROPERTIES),
                    prop(
                        "insertedFromYaml",
                        escape("C:\\temp\\new${LINES_SEPARATOR}docker run \\"),
                        PROPERTIES,
                    ),
                    prop("escapedInSource", "a\\\\b", PROPERTIES),
                    prop("continuation", "1\\${LINES_SEPARATOR}2", PROPERTIES),
                    prop("simple", "x", PROPERTIES),
                ),
            )
        assertFalse(diffFile(file).exists())
    }

    @Test
    fun storeDiffOfChangedValues() {
        val file =
            build(
                "application.properties",
                listOf(prop("multiline", "first${LINES_SEPARATOR}second", PROPERTIES)),
            )
        ConfigDiff()
            .storeDiffFor(
                file,
                listOf(prop("multiline", "first${LINES_SEPARATOR}changed", PROPERTIES)),
            )

        assertTrue(diffFile(file).readText().contains("first\\nsecond -> first\\nchanged"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["old", "123", "a: b", "first\nsecond", "\nfirst\n\n"])
    fun storeChangedYamlValuesAsValidYaml(value: String) {
        val old = YamlScalars.stringValue(value)
        val current = YamlScalars.stringValue("$value changed")
        val file = build("application.yaml", listOf(prop("value", old, YAML)))
        ConfigDiff().storeDiffFor(file, listOf(prop("value", current, YAML)))

        val diff = Yaml().load<Map<String, Any?>>(diffFile(file).readText())
        assertEquals("$old -> $current", diff["value"])
    }

    private fun build(fileName: String, properties: List<Property>): File {
        val file = dir.resolve(fileName).toFile()
        configIo().writeTo(file).write(properties)
        ConfigDiff().storeDiffFor(file, properties)
        return file
    }

    private fun diffFile(file: File): File = File(file.parentFile, "diff-${file.name}")

    private fun prop(key: String, value: String, format: ConfigFormat): Property =
        property(key, value, format, null)
}
