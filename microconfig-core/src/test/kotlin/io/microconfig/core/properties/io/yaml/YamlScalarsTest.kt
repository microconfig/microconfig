package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.io.properties.PropertiesText.unescape
import io.microconfig.core.properties.io.yaml.YamlScalars.fromPropertiesText
import io.microconfig.core.properties.io.yaml.YamlScalars.stringValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

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
}
