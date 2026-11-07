package io.microconfig.core

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.serializers.PropertySerializers
import io.microconfig.core.properties.templates.TemplatePattern
import io.microconfig.core.properties.templates.impl.BinaryTemplate
import io.microconfig.core.properties.templates.impl.StringTemplate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.BiConsumer

class SerializationBoundariesTest {
    @TempDir
    lateinit var directory: Path

    @ParameterizedTest
    @ValueSource(strings = ["C:\\temp\\new", "\\u0041", "\\\\server\\share", "first\nsecond", "a\\\nb", " \tleading", "'quoted'", "a\rb\u000C"])
    fun preserveValuesInPropertiesForEveryRepresentation(text: String) {
        val writer = configIo().writeTo(directory.resolve("result.properties").toFile())
        ValueFormat.entries.forEach { format ->
            val value = when (format) {
                ValueFormat.TEXT -> text
                ValueFormat.PROPERTIES -> PropertiesText.escape(text)
                ValueFormat.YAML -> YamlScalars.doubleQuoted(text)
            }
            val property = PropertyImpl.property("value", value, ConfigFormat.PROPERTIES, null, format)
            val serialized = writer.serialize(listOf(property))
            val actual = Properties().apply { load(StringReader(serialized)) }.getProperty("value")
            assertEquals(text, actual, "format=$format, serialized=$serialized")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["a\\u002eb", "a\\ b.child", "quote\\\"name.child"])
    fun keepKeysIndependentFromValueRepresentation(key: String) {
        val source = directory.resolve("application.properties").toFile()
        Files.writeString(source.toPath(), "$key=x")
        val io = configIo()
        val original = io.readFrom(source).properties("app", "dev").single() as PropertyImpl
        val writer = io.writeTo(directory.resolve("result.yaml").toFile())
        val expected = Yaml().load<Map<String, Any>>(writer.serialize(listOf(original)))

        ValueFormat.entries.forEach { format ->
            val property = original.copy(valueFormat = format)
            assertEquals(expected, Yaml().load<Map<String, Any>>(writer.serialize(listOf(property))), "$format")
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun acceptEquivalentTemplatesAfterPreparation(binary: Boolean) {
        val source = directory.resolve("source").toFile()
        val destination = directory.resolve("app/template").toFile()
        val bytes = if (binary) byteArrayOf(0, -1, 1) else "text".toByteArray()
        Files.write(source.toPath(), bytes)
        val template = if (binary) BinaryTemplate("t", source, destination, bytes)
        else StringTemplate("t", source, destination, TemplatePattern.defaultPattern().placeholderPattern, "text")
        val equivalent = if (binary) BinaryTemplate("t", source, destination, bytes.copyOf())
        else StringTemplate("t", source, destination, TemplatePattern.defaultPattern().placeholderPattern, "text")
        val values = TypedPropertiesImpl(APPLICATION, "app", "dev", emptyMap(), listOf(template))
        val serializer = PropertySerializers.toFileIn(directory.toFile(), BiConsumer { _, _ -> })
            .prepare(listOf(values))

        values.withTemplates(listOf(equivalent)).save(serializer)

        assertArrayEquals(bytes, Files.readAllBytes(destination.toPath()))
    }

    @Test
    fun rejectChangedTemplateContentAfterPreparation() {
        val source = directory.resolve("source").toFile()
        val destination = directory.resolve("app/template").toFile()
        val template = BinaryTemplate("t", source, destination, byteArrayOf(1))
        val changed = BinaryTemplate("t", source, destination, byteArrayOf(2))
        val values = TypedPropertiesImpl(APPLICATION, "app", "dev", emptyMap(), listOf(template))
        val serializer = PropertySerializers.toFileIn(directory.toFile(), BiConsumer { _, _ -> })
            .prepare(listOf(values))

        assertThrows(IllegalArgumentException::class.java) {
            values.withTemplates(listOf(changed)).save(serializer)
        }
        assertFalse(destination.exists())
    }

    @Test
    fun rejectAChangedOutputFormatAfterPreparation() {
        val original = PropertyImpl.property("value", "same", ConfigFormat.PROPERTIES, null)
        val changed = PropertyImpl.property("value", "same", ConfigFormat.YAML, null)
        val values = TypedPropertiesImpl(APPLICATION, "app", "dev", mapOf("value" to original), emptyList())
        val serializer = PropertySerializers.toFileIn(directory.toFile(), BiConsumer { _, _ -> })
            .prepare(listOf(values))

        assertThrows(IllegalArgumentException::class.java) {
            serializer.serialize(listOf(changed), emptyList(), APPLICATION, "app", "dev")
        }
        assertFalse(Files.exists(directory.resolve("app/application.properties")))
        assertFalse(Files.exists(directory.resolve("app/application.yaml")))
    }
}
