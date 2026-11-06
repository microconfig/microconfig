package io.microconfig.core.properties.io.properties

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.io.properties.PropertiesText.escape
import io.microconfig.core.properties.io.properties.impl.PropertiesWriter
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import java.io.File
import java.io.StringReader
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PropertiesWriterTest {
    val writer: PropertiesWriter = PropertiesWriter(File("unused.properties"))

    @ParameterizedTest
    @ValueSource(strings = ["  a\nb", "head   a\nb tail", "a\n\nb", "\tindented", " ", "\n  a"])
    fun javaReadsBackMultilineAndIndentedValues(value: String) {
        val serialized: String =
            writer.serialize(listOf(prop("key", value.replace("\n", LINES_SEPARATOR))))
        assertEquals(value, load(serialized).getProperty("key"))
    }

    @Test
    fun keepContinuationsOfPropertiesSources() {
        val continued: String = "first\\" + LINES_SEPARATOR + "second"
        assertEquals("key=" + continued, writer.serialize(listOf(prop("key", continued))))
    }

    @Test
    fun escapeLineBreakAfterEvenBackslashes() {
        val value: String = "C:\\\\" + LINES_SEPARATOR + "next"
        assertEquals(
            "C:\\\nnext",
            load(writer.serialize(listOf(prop("key", value)))).getProperty("key"),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "C:\\temp\\new",
                "docker run \\\n  --rm img",
                "\\d+",
                "  a\nb",
                "\u000Cform feed",
                "a\\",
                "\\\\server\\share",
            ]
    )
    fun javaReadsBackTextEscapedByTheResolver(value: String) {
        val inserted: Property = prop("key", escape(value.replace("\n", LINES_SEPARATOR)))
        assertEquals(value, load(writer.serialize(listOf(inserted))).getProperty("key"))
    }

    @Test
    fun escapeLeadingFormFeedOfPropertiesValues() {
        assertEquals(
            "\u000Cx",
            load(writer.serialize(listOf(prop("key", "\u000Cx")))).getProperty("key"),
        )
    }

    @Test
    fun serializedValueIsTheValueWrittenToTheFile() {
        val yamlValue: Property = property("key", "C:\\temp" + LINES_SEPARATOR + "next", YAML, null)
        assertEquals(
            "key=" + writer.serializedValue(yamlValue),
            writer.serialize(listOf(yamlValue)),
        )
    }

    @Test
    fun keepPlainValuesAsIs() {
        assertEquals(
            "key=value with spaces",
            writer.serialize(listOf(prop("key", "value with spaces"))),
        )
    }

    private fun prop(key: String, value: String): Property {
        return property(key, value, PROPERTIES, null)
    }

    private fun load(serialized: String): Properties {
        val properties: Properties = Properties()
        properties.load(StringReader(serialized))
        return properties
    }
}
