package io.microconfig.core.properties.io.properties;

import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.io.properties.PropertiesText.escape;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesWriterTest {
    private final PropertiesWriter writer = new PropertiesWriter(new File("unused.properties"));

    @ParameterizedTest
    @ValueSource(strings = {"  a\nb", "head   a\nb tail", "a\n\nb", "\tindented", " ", "\n  a"})
    void javaReadsBackMultilineAndIndentedValues(String value) throws IOException {
        String serialized = writer.serialize(List.of(prop("key", value.replace("\n", LINES_SEPARATOR))));

        assertEquals(value, load(serialized).getProperty("key"));
    }

    @Test
    void keepContinuationsOfPropertiesSources() {
        String continued = "first\\" + LINES_SEPARATOR + "second";

        assertEquals("key=" + continued, writer.serialize(List.of(prop("key", continued))));
    }

    @Test
    void escapeLineBreakAfterEvenBackslashes() throws IOException {
        String value = "C:\\\\" + LINES_SEPARATOR + "next";

        assertEquals("C:\\\nnext", load(writer.serialize(List.of(prop("key", value)))).getProperty("key"));
    }

    // a YAML value inserted into a .properties value by a placeholder is escaped by the resolver and must be kept as is
    @ParameterizedTest
    @ValueSource(strings = {"C:\\temp\\new", "docker run \\\n  --rm img", "\\d+", "  a\nb", "\fform feed", "a\\", "\\\\server\\share"})
    void javaReadsBackTextEscapedByTheResolver(String value) throws IOException {
        Property inserted = prop("key", escape(value.replace("\n", LINES_SEPARATOR)));

        assertEquals(value, load(writer.serialize(List.of(inserted))).getProperty("key"));
    }

    @Test
    void escapeLeadingFormFeedOfPropertiesValues() throws IOException {
        assertEquals("\fx", load(writer.serialize(List.of(prop("key", "\fx")))).getProperty("key"));
    }

    @Test
    void serializedValueIsTheValueWrittenToTheFile() {
        Property yamlValue = property("key", "C:\\temp" + LINES_SEPARATOR + "next", YAML, null);

        assertEquals("key=" + writer.serializedValue(yamlValue), writer.serialize(List.of(yamlValue)));
    }

    @Test
    void keepPlainValuesAsIs() {
        assertEquals("key=value with spaces", writer.serialize(List.of(prop("key", "value with spaces"))));
    }

    private Property prop(String key, String value) {
        return property(key, value, PROPERTIES, null);
    }

    private Properties load(String serialized) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(serialized));
        return properties;
    }
}
