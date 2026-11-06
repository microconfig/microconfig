package io.microconfig.core.properties.io.properties;

import io.microconfig.core.properties.DeclaringComponentImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.io.properties.PropertiesText.escape;
import static io.microconfig.core.properties.io.properties.PropertiesText.isPropertiesText;
import static io.microconfig.core.properties.io.properties.PropertiesText.unescape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesTextTest {
    @ParameterizedTest
    @ValueSource(strings = {"C:\\temp\\new", "docker run \\\n  --rm img", "\\d+", "a\nb", "a\rb", "a\r\nb", "tab\there", "a\\", "plain"})
    void javaReadsEscapedTextBack(String plain) throws IOException {
        assertEquals(plain, load("key=" + escape(plain)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x\\\\y", "first\\\n  second", "\\u0041", "a\\tb", "a\\nb", "a\\=b", "\\  leading", "C:\\\\temp", "trailing\\"})
    void unescapeLikePropertiesLoad(String propertiesText) throws IOException {
        assertEquals(load("key=" + propertiesText), unescape(propertiesText));
    }

    // Properties.load fails on it, the build keeps the text instead
    @Test
    void keepMalformedUnicodeEscape() {
        assertEquals("uZZZZ", unescape("\\uZZZZ"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\temp\\new", "multi\nline", "a\rb", "\\"})
    void unescapeReversesEscape(String plain) {
        assertEquals(plain, unescape(escape(plain)));
    }

    @Test
    void onlyValuesOfPropertiesFilesArePropertiesText() {
        assertTrue(isPropertiesText(fileSource(new File("application.properties"), 0, false, "app", "dev")));
        assertFalse(isPropertiesText(fileSource(new File("application.yaml"), 0, true, "app", "dev")));
        assertFalse(isPropertiesText(new DeclaringComponentImpl("app", "component", "dev")));
    }

    private String load(String line) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(line));
        return properties.getProperty("key");
    }
}
