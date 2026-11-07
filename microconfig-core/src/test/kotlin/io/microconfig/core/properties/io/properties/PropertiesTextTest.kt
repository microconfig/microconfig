package io.microconfig.core.properties.io.properties

import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.io.properties.PropertiesText.escape
import io.microconfig.core.properties.io.properties.PropertiesText.isPropertiesText
import io.microconfig.core.properties.io.properties.PropertiesText.unescape
import java.io.File
import java.io.StringReader
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PropertiesTextTest {
    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "C:\\temp\\new",
                "docker run \\\n  --rm img",
                "\\d+",
                "a\nb",
                "a\rb",
                "tab\there",
                "a\\",
                "plain",
            ]
    )
    fun javaReadsEscapedTextBack(plain: String) {
        assertEquals(plain, load("key=" + escape(plain)))
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "x\\\\y",
                "first\\\n  second",
                "\\u0041",
                "a\\tb",
                "a\\nb",
                "a\\=b",
                "\\  leading",
                "C:\\\\temp",
                "trailing\\",
            ]
    )
    fun unescapeLikePropertiesLoad(propertiesText: String) {
        assertEquals(load("key=" + propertiesText), unescape(propertiesText))
    }

    @Test
    fun writeWindowsLineBreakAsLineBreak() {
        assertEquals("a\nb", load("key=" + escape("a\r\nb")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["a\r\nb", "a\r\n\nb\r", "\r\n", "\r", "\n", "literal\\r\\n\r\nnext"])
    fun encodeValueWithoutChangingLineBreaks(value: String) {
        assertEquals(value, load("key=" + PropertiesText.escapeValue(value)))
    }

    @Test
    fun keepMalformedUnicodeEscape() {
        assertEquals("uZZZZ", unescape("\\uZZZZ"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["C:\\temp\\new", "multi\nline", "a\rb", "\\"])
    fun unescapeReversesEscape(plain: String) {
        assertEquals(plain, unescape(escape(plain)))
    }

    @Test
    fun onlyValuesOfPropertiesFilesArePropertiesText() {
        assertTrue(
            isPropertiesText(fileSource(File("application.properties"), 0, false, "app", "dev"))
        )
        assertFalse(isPropertiesText(fileSource(File("application.yaml"), 0, true, "app", "dev")))
        assertFalse(isPropertiesText(DeclaringComponentImpl("app", "component", "dev")))
    }

    private fun load(line: String): String {
        val properties: Properties = Properties()
        properties.load(StringReader(line))
        return properties.getProperty("key")
    }
}
