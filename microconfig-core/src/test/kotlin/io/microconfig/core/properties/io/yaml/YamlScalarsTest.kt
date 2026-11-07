package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.io.properties.PropertiesText.unescape
import io.microconfig.core.properties.io.yaml.YamlScalars.fromPropertiesText
import io.microconfig.core.properties.io.yaml.YamlScalars.stringValue
import io.microconfig.core.properties.io.yaml.impl.SnakeYaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

class YamlScalarsTest {
    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "\\#ff0000",
                "b\\: c",
                "C:\\\\temp",
                "!tag",
                "*.log",
                "@at",
                "a # b",
                "- item",
                "{x}",
                "[x]",
                "\'quoted\'",
                "null",
                "~",
                "yes",
                "010",
                "hello world",
            ]
    )
    fun yamlReadsTheSameText(propertiesText: String) {
        val value: Any? =
            Yaml().load<Map<*, *>>("key: " + fromPropertiesText(propertiesText)).get("key")
        assertEquals(unescape(propertiesText), value)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value =
            [
                "8080 | 8080",
                "true | true",
                "1.5 | 1.5",
                "hello world | hello world",
                "C:\\\\data | C:\\data",
                "\\#1 | \"#1\"",
            ],
    )
    fun quoteOnlyWhatYamlWouldReadDifferently(propertiesText: String, yamlValue: String) {
        assertEquals(yamlValue, fromPropertiesText(propertiesText))
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "hello, world",
                "left[right]",
                "left{right}",
                "a: b",
                "a:b",
                "-",
                "\ud83d\ude42, cat",
                "hello world",
            ]
    )
    fun propertiesTextIsOneScalarInBlockAndFlow(text: String) {
        val serialized: String = fromPropertiesText(text)
        assertEquals(text, Yaml().load<Map<*, *>>("key: " + serialized).get("key"))
        assertEquals(listOf(text), Yaml().load("[" + serialized + "]"))
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "hello, world",
                "left[right]",
                "left{right}",
                "a: b",
                "-",
                "\ud83d\ude42, cat",
                "123",
                "true",
                "null",
            ]
    )
    fun stringValueStaysOneStringInBlockAndFlow(text: String) {
        val serialized: String = stringValue(text)
        assertEquals(text, Yaml().load<Map<*, *>>("key: " + serialized).get("key"))
        assertEquals(listOf(text), Yaml().load("[" + serialized + "]"))
    }

    @ParameterizedTest
    @MethodSource("quotedValues")
    fun decodeQuotedScalarsIncludingEmptyAndExplicitlyTaggedValues(value: String, expected: String) {
        assertEquals(expected, YamlScalars.quotedTextOrNull(value))
        assertEquals(expected, YamlScalars.unquote(value))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "plain", "null", "[one, two]", "{key: value}",
        "'unterminated", "\"unterminated", "|-\n  first\n  second\n",
        ">\n  first\n  second\n", "!!timestamp 2001-12-15", "!custom plain"])
    fun preserveUnquotedMalformedAndBlockValues(value: String) {
        assertNull(YamlScalars.quotedTextOrNull(value))
        assertEquals(value, YamlScalars.unquote(value))
    }

    @ParameterizedTest
    @MethodSource("canonicalValues")
    fun canonicalizeLogicalScalarValuesWithoutChangingTheirKeyTypes(value: String, tag: Tag, expected: String) {
        val node = SnakeYaml.parse(value) as ScalarNode

        assertEquals(expected, YamlScalars.canonicalScalarValue(node))
        val key = SnakeYaml.parse(YamlScalars.canonicalKey(node)) as ScalarNode
        assertEquals(tag, key.tag)
        assertEquals(expected, key.value)
    }

    companion object {
        @JvmStatic
        fun quotedValues(): List<Arguments> = listOf(
            Arguments.of("''", ""),
            Arguments.of("\"\"", ""),
            Arguments.of("'it''s true'", "it's true"),
            Arguments.of("\"first\\r\\n\\\"quote\\\"\\\\tail\"", "first\r\n\"quote\"\\tail"),
            Arguments.of("'😀 Привет'", "😀 Привет"),
            Arguments.of("!!timestamp '2001-12-15'", "2001-12-15"),
            Arguments.of("!!binary 'aGVsbG8='", "aGVsbG8="),
            Arguments.of("!custom 'text'", "text"),
            Arguments.of("!!float '1.5'", "1.5"),
        )

        @JvmStatic
        fun canonicalValues(): List<Arguments> = listOf(
            Arguments.of("010", Tag.INT, "8"),
            Arguments.of("0xA", Tag.INT, "10"),
            Arguments.of("1_000", Tag.INT, "1000"),
            Arguments.of("1:00", Tag.INT, "60"),
            Arguments.of("!!int '010'", Tag.INT, "8"),
            Arguments.of("1_000.5", Tag.FLOAT, "1000.5"),
            Arguments.of(".5", Tag.FLOAT, "0.5"),
            Arguments.of("1.25e+2", Tag.FLOAT, "125.0"),
            Arguments.of(".inf", Tag.FLOAT, ".inf"),
            Arguments.of("-.Inf", Tag.FLOAT, "-.inf"),
            Arguments.of(".NaN", Tag.FLOAT, ".NaN"),
            Arguments.of("YES", Tag.BOOL, "true"),
            Arguments.of("off", Tag.BOOL, "false"),
            Arguments.of("~", Tag.NULL, "null"),
            Arguments.of("", Tag.NULL, "null"),
            Arguments.of("'010'", Tag.STR, "010"),
            Arguments.of("'a.b'", Tag.STR, "a.b"),
            Arguments.of("!!timestamp '2001-12-15T00:00:00Z'", Tag.TIMESTAMP, "2001-12-15T00:00:00Z"),
            Arguments.of("!custom 'text'", Tag("!custom"), "text"),
            Arguments.of("!!int 'not-a-number'", Tag.INT, "not-a-number"),
            Arguments.of("!!bool 'not-a-boolean'", Tag.BOOL, "not-a-boolean"),
        )
    }
}
