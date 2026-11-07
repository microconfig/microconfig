package io.microconfig.core

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream

class KeyFormatBoundariesTest {
    @TempDir
    lateinit var directory: Path

    @ParameterizedTest
    @MethodSource("yamlKeys")
    fun writeYamlKeySyntaxAsTheLogicalPropertiesKey(key: String, expected: String) {
        val property = PropertyImpl.property(
            key, "payload", ConfigFormat.PROPERTIES, null,
            ValueFormat.TEXT, ConfigFormat.YAML
        )
        val serialized = configIo().writeTo(directory.resolve("result.properties").toFile())
            .serialize(listOf(property))

        val characters = Properties().apply { load(StringReader(serialized)) }
        val bytes = Properties().apply { load(ByteArrayInputStream(serialized.toByteArray(Charsets.UTF_8))) }

        assertEquals(mapOf(expected to "payload"), characters, serialized)
        assertEquals(characters, bytes, serialized)
    }

    @ParameterizedTest
    @ValueSource(strings = ["a\\ b", "a\\:b", "a\\=b", "slash\\\\tail", "a\\u002eb", "\\#name", "multi\\\n  line"])
    fun preserveAlreadyEncodedPropertiesKeysAndContinuedValues(key: String) {
        val value = "first\\\n  second"
        val property = PropertyImpl.property(
            key, value, ConfigFormat.PROPERTIES, null,
            ValueFormat.PROPERTIES, ConfigFormat.PROPERTIES
        )
        val serialized = configIo().writeTo(directory.resolve("result.properties").toFile())
            .serialize(listOf(property))

        assertEquals("$key=$value", serialized)
        assertEquals(
            mapOf(PropertiesText.unescape(key) to "firstsecond"),
            Properties().apply { load(StringReader(serialized)) })
    }

    @ParameterizedTest
    @ValueSource(strings = ["a\\ b", "a\\:b", "a\\=b", "slash\\\\tail", "a\\u0041", "literal\\\\u0041"])
    fun preserveLegacyFactoryEncodedKeysForPropertiesAndYaml(key: String) {
        val property = PropertyImpl.property(key, "payload", ConfigFormat.PROPERTIES, null)
        val io = configIo()
        val properties = io.writeTo(directory.resolve("result.properties").toFile()).serialize(listOf(property))
        val yaml = io.writeTo(directory.resolve("result.yaml").toFile()).serialize(listOf(property))
        val expected = mapOf(PropertiesText.unescape(key) to "payload")

        assertEquals("$key=payload", properties)
        assertEquals(expected, Properties().apply { load(StringReader(properties)) })
        assertEquals(expected, Yaml().load<Map<String, String>>(yaml))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.5", "0x10", "YES", "~", "section.1.5"])
    fun findYamlKeysIndependentlyFromTheOutputFormat(query: String) {
        val source = directory.resolve("input.yaml").toFile()
        Files.writeString(source.toPath(), "1.5: float\n0x10: hex\ntrue: flag\nnull: empty\nsection:\n  1.5: nested\n")
        val read = configIo().readFrom(source).properties("app", "dev").map { it as PropertyImpl }
        val yaml = typed(read)
        val properties = typed(read.map { it.copy(configFormat = ConfigFormat.PROPERTIES) })

        assertTrue(yaml.getPropertyWithKey(query).isPresent, query)
        assertEquals(
            yaml.getPropertyWithKey(query).orElseThrow().value,
            properties.getPropertyWithKey(query).orElseThrow().value, query
        )
    }

    @Test
    fun groupYamlFloatKeyChildrenIndependentlyFromTheOutputFormat() {
        val source = directory.resolve("input.yaml").toFile()
        Files.writeString(source.toPath(), "1.5:\n  child: nested\n  other: retained\n")
        val properties = configIo().readFrom(source).properties("app", "dev")
            .map { (it as PropertyImpl).copy(configFormat = ConfigFormat.PROPERTIES) }

        val grouped = typed(properties).getPropertyWithKey("1.5.*").orElseThrow()

        assertEquals(mapOf("child" to "nested", "other" to "retained"), Yaml().load<Map<String, String>>(grouped.value))
    }

    @ParameterizedTest
    @MethodSource("collidingYamlKeys")
    fun rejectYamlKeysThatCollapseToTheSamePropertiesKey(firstKey: String, secondKey: String) {
        val first = PropertyImpl.property(
            firstKey, "first", ConfigFormat.PROPERTIES, null,
            ValueFormat.TEXT, ConfigFormat.YAML
        )
        val second = PropertyImpl.property(
            secondKey, "second", ConfigFormat.PROPERTIES, null,
            ValueFormat.TEXT, ConfigFormat.YAML
        )
        val target = directory.resolve("result.properties")
        Files.writeString(target, "retained")

        val error = assertThrows(IllegalArgumentException::class.java) {
            configIo().writeTo(target.toFile()).write(listOf(first, second))
        }

        assertTrue(error.message.orEmpty().contains("same Properties key"), error.message)
        assertEquals("retained", Files.readString(target))
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun rejectMixedEncodedAndYamlKeysThatProduceTheSamePropertiesKey(yamlFirst: Boolean) {
        val raw = PropertyImpl.property("a\\u0041", "first", ConfigFormat.PROPERTIES, null)
        val yaml = PropertyImpl.property(
            "aA", "second", ConfigFormat.PROPERTIES, null,
            ValueFormat.TEXT, ConfigFormat.YAML
        )
        val properties = if (yamlFirst) listOf(yaml, raw) else listOf(raw, yaml)

        assertThrows(IllegalArgumentException::class.java) {
            configIo().writeTo(directory.resolve("result.properties").toFile()).serialize(properties)
        }
    }

    @Test
    fun preserveLegacyDuplicateRawKeyBehavior() {
        val first = PropertyImpl.property("a\\u0041", "first", ConfigFormat.PROPERTIES, null)
        val second = PropertyImpl.property("aA", "second", ConfigFormat.PROPERTIES, null)
        val serialized = configIo().writeTo(directory.resolve("result.properties").toFile())
            .serialize(listOf(first, second))

        assertTrue(serialized.contains("a\\u0041=first"), serialized)
        assertTrue(serialized.contains("aA=second"), serialized)
        assertEquals(mapOf("aA" to "second"), Properties().apply { load(StringReader(serialized)) })
    }

    private fun typed(properties: List<PropertyImpl>) = TypedPropertiesImpl(
        APPLICATION, "app", "dev", properties.associateBy { it.key }, emptyList(),
    )

    companion object {
        @JvmStatic
        fun collidingYamlKeys(): Stream<Arguments> = Stream.of(
            Arguments.of("1", "'1'"),
            Arguments.of("a.b", "'a.b'"),
        )

        @JvmStatic
        fun yamlKeys(): Stream<Arguments> = Stream.of(
            Arguments.of("db:host", "db:host"),
            Arguments.of("'db=host'", "db=host"),
            Arguments.of("space key", "space key"),
            Arguments.of("' leading '", " leading "),
            Arguments.of("'a\\b'", "a\\b"),
            Arguments.of("'a''b'", "a'b"),
            Arguments.of("\"a\\\"b\"", "a\"b"),
            Arguments.of("'literal.key'", "literal.key"),
            Arguments.of("section.'literal.key'", "section.literal.key"),
            Arguments.of("section.!!float \"1.5\"", "section.1.5"),
            Arguments.of("!!float \"1.5\"", "1.5"),
            Arguments.of("0x10", "16"),
            Arguments.of("true", "true"),
            Arguments.of("~", "null"),
            Arguments.of("!!str 01", "01"),
            Arguments.of("''", ""),
            Arguments.of("'#head!'", "#head!"),
            Arguments.of("\"tab\\tname\"", "tab\tname"),
            Arguments.of("\"line\\nname\"", "line\nname"),
            Arguments.of("\"return\\rname\"", "return\rname"),
            Arguments.of("\"form\\fname\"", "form\u000Cname"),
            Arguments.of("'ключ🙂'", "ключ🙂"),
            Arguments.of("'\\u0041'", "\\u0041"),
        )
    }
}
