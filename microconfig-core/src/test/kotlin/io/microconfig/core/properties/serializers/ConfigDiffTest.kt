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
import java.io.StringReader
import java.nio.file.Path
import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
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

    @ParameterizedTest
    @MethodSource("yamlDiffKeys")
    fun preserveYamlKeyStructureForAddedAndRemovedValues(key: String, expectedFirstKey: String,
        expectedRest: Map<String, Any>?) {
        val baseline = listOf(prop("retained", "baseline", YAML))
        val added = prop(key, "payload", YAML)
        val file = build("application.yaml", baseline)
        ConfigDiff().storeDiffFor(file, baseline + added)
        val expectedValue: Any = expectedRest ?: "payload"
        assertEquals(mapOf("+$expectedFirstKey" to expectedValue),
            Yaml().load<Map<String, Any>>(diffFile(file).readText()))

        configIo().writeTo(file).write(baseline + added)
        ConfigDiff().storeDiffFor(file, baseline)
        assertEquals(mapOf("-$expectedFirstKey" to expectedValue),
            Yaml().load<Map<String, Any>>(diffFile(file).readText()))
    }

    @Test
    fun keepEncodedPropertiesKeysInDiffs() {
        val baseline = listOf(prop("retained", "baseline", PROPERTIES))
        val added = prop("a\\u002eb", "payload", PROPERTIES)
        val file = build("application.properties", baseline)
        ConfigDiff().storeDiffFor(file, baseline + added)
        assertEquals("+a\\u002eb=payload", diffFile(file).readText())
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun keepRealMarkerKeysDistinctFromAddedAndRemovedLabels(extension: String) {
        val format = if (extension == "yaml") YAML else PROPERTIES
        fun keyed(name: String, value: String) = prop(
            if (format == YAML) YamlScalars.doubleQuoted(name) else name.replace("\\", "\\\\"),
            value, format,
        )
        val before = listOf(keyed("+x", "oldPlus"), keyed("-y", "oldMinus"),
            keyed("\\+x", "oldSlash"), keyed("y", "removed"))
        val after = listOf(keyed("+x", "newPlus"), keyed("-y", "newMinus"),
            keyed("\\+x", "newSlash"), keyed("x", "added"))
        val file = build("application.$extension", before)

        ConfigDiff().storeDiffFor(file, after)

        val expected = mapOf("\\+x" to "oldPlus -> newPlus", "\\-y" to "oldMinus -> newMinus",
            "\\\\+x" to "oldSlash -> newSlash", "+x" to "added", "-y" to "removed")
        val diff = diffFile(file).readText()
        val actual = if (format == YAML) Yaml().load<Map<String, String>>(diff)
            else Properties().apply { load(StringReader(diff)) }
        assertEquals(expected, actual, diff)
        configIo().writeTo(file).write(after)
        ConfigDiff().storeDiffFor(file, after)
        assertFalse(diffFile(file).exists())
    }

    @Test
    fun escapeChangedPropertiesMarkerKeysAfterDecodingTheirSpelling() {
        val file = build("application.properties", listOf(prop("\\u002bx", "old", PROPERTIES)))

        ConfigDiff().storeDiffFor(file, listOf(prop("\\u002bx", "new", PROPERTIES), prop("x", "added", PROPERTIES)))

        val actual = Properties().apply { load(StringReader(diffFile(file).readText())) }
        assertEquals(mapOf("\\+x" to "old -> new", "+x" to "added"), actual)
    }

    @ParameterizedTest
    @MethodSource("typedAndStringKeys")
    fun retainChangesForTypedAndStringYamlKeys(typedKey: String, stringKey: String,
        typedLabel: String, stringLabel: String) {
        val baseline = listOf(prop("retained", "baseline", YAML))
        val changes = listOf(prop(typedKey, "typed", YAML), prop(stringKey, "string", YAML))
        val file = build("application.yaml", baseline)

        ConfigDiff().storeDiffFor(file, baseline + changes)
        assertEquals(mapOf("+$typedLabel" to "typed", "+$stringLabel" to "string"),
            Yaml().load<Map<String, String>>(diffFile(file).readText()))

        configIo().writeTo(file).write(baseline + changes)
        ConfigDiff().storeDiffFor(file, baseline)
        assertEquals(mapOf("-$typedLabel" to "typed", "-$stringLabel" to "string"),
            Yaml().load<Map<String, String>>(diffFile(file).readText()))
    }

    @Test
    fun retainTypedAndStringFirstSegmentsOfNestedYamlKeys() {
        val baseline = listOf(prop("retained", "baseline", YAML))
        val changes = listOf(prop("1.child", "typed", YAML), prop("'1'.child", "string", YAML))
        val file = build("application.yaml", baseline)

        ConfigDiff().storeDiffFor(file, baseline + changes)

        assertEquals(mapOf("+1" to mapOf("child" to "typed"), "+'1'" to mapOf("child" to "string")),
            Yaml().load<Map<String, Any>>(diffFile(file).readText()))
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

    companion object {
        @JvmStatic
        fun typedAndStringKeys(): Stream<Arguments> = Stream.of(
            Arguments.of("1", "'1'", "1", "'1'"),
            Arguments.of("true", "'true'", "true", "'true'"),
            Arguments.of("null", "'null'", "null", "'null'"),
            Arguments.of("!!float \"1.5\"", "'1.5'", "1.5", "'1.5'"),
            Arguments.of("'1'", "'''1'''", "'1'", "'''1'''"),
        )

        @JvmStatic
        fun yamlDiffKeys(): Stream<Arguments> = Stream.of(
            Arguments.of("\"a.b\"", "a.b", null),
            Arguments.of("'a.b'", "a.b", null),
            Arguments.of("\"a\\\".b\"", "a\".b", null),
            Arguments.of("\"a.b\".child", "a.b", mapOf("child" to "payload")),
            Arguments.of("section.\"a.b\"", "section", mapOf("a.b" to "payload")),
            Arguments.of("section.child", "section", mapOf("child" to "payload")),
            Arguments.of("!!float \"1.5\"", "1.5", null),
            Arguments.of("'true'", "'true'", null),
            Arguments.of("'\\u0041'", "\\u0041", null),
        )
    }
}
