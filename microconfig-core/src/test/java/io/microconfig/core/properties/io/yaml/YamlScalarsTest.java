package io.microconfig.core.properties.io.yaml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static io.microconfig.core.properties.io.properties.PropertiesText.unescape;
import static io.microconfig.core.properties.io.yaml.YamlScalars.fromPropertiesText;
import static io.microconfig.core.properties.io.yaml.YamlScalars.stringValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlScalarsTest {
    // whatever a .properties value is, YAML reads back the text Properties.load would read
    @ParameterizedTest
    @ValueSource(strings = {"\\#ff0000", "b\\: c", "C:\\\\temp", "!tag", "*.log", "@at", "a # b", "- item", "{x}", "[x]", "'quoted'", "null", "~", "yes", "010", "hello world"})
    void yamlReadsTheSameText(String propertiesText) {
        Object value = new Yaml().<Map<?, ?>>load("key: " + fromPropertiesText(propertiesText)).get("key");

        assertEquals(unescape(propertiesText), value);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"8080 | 8080", "true | true", "1.5 | 1.5", "hello world | hello world", "C:\\\\data | C:\\data", "\\#1 | \"#1\""})
    void quoteOnlyWhatYamlWouldReadDifferently(String propertiesText, String yamlValue) {
        assertEquals(yamlValue, fromPropertiesText(propertiesText));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello, world", "left[right]", "left{right}", "a: b", "a:b", "-", "🙂, cat", "hello world"})
    void propertiesTextIsOneScalarInBlockAndFlow(String text) {
        String serialized = fromPropertiesText(text);

        assertEquals(text, new Yaml().<Map<?, ?>>load("key: " + serialized).get("key"));
        assertEquals(List.of(text), new Yaml().load("[" + serialized + "]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello, world", "left[right]", "left{right}", "a: b", "-", "🙂, cat", "123", "true", "null"})
    void stringValueStaysOneStringInBlockAndFlow(String text) {
        String serialized = stringValue(text);

        assertEquals(text, new Yaml().<Map<?, ?>>load("key: " + serialized).get("key"));
        assertEquals(List.of(text), new Yaml().load("[" + serialized + "]"));
    }
}
