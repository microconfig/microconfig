package io.microconfig.core.properties.templates.definition.parser

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplatePattern.Companion.defaultPattern
import io.microconfig.core.properties.templates.definition.parser.impl.FromToNotationParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import java.io.File

class FromToNotationParserTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun singleFile() {
        val templateDefinitions: ArrayList<TemplateDefinition> =
            parseProperties(
                createProperty("mc.template.name.fromFile", "a.yaml"),
                createProperty("mc.template.name.toFile", "b.yaml"),
            )
        assertTemplateDefinition("a.yaml", "b.yaml", templateDefinitions.get(0))
    }

    private fun createProperty(key: String, file: String): Property {
        return property(
            key,
            tempDir.toString() + "/" + file,
            YAML,
            mock(DeclaringComponent::class.java),
        )
    }

    private fun parseProperties(vararg properties: Property): ArrayList<TemplateDefinition> {
        return ArrayList(FromToNotationParser(defaultPattern()).parse(properties.toList()))
    }

    private fun assertTemplateDefinition(
        from: String,
        to: String,
        templateDefinition: TemplateDefinition,
    ) {
        assertEquals("name", templateDefinition.templateName)
        assertEquals(
            (tempDir.toString() + "/" + from).replace('\\', '/'),
            (templateDefinition.fromFile.toString()).replace('\\', '/'),
        )
        assertEquals(
            (tempDir.toString() + "/" + to).replace('\\', '/'),
            (templateDefinition.toFile.toString()).replace('\\', '/'),
        )
    }
}
