package io.microconfig.core.properties.templates.definition.parser

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplatePattern.Companion.defaultPattern
import io.microconfig.core.properties.templates.definition.parser.impl.SquareBracketsNotationParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import java.io.File

class SquareBracketsNotationParserTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun multipleTemplateNames() {
        val templateDefinitions: List<TemplateDefinition> =
            parseArrowProperty("mc.template.[name1,name2]", "a.yaml", "b.yaml")
        assertTemplateDefinition("name1", "a.yaml", "b.yaml", templateDefinitions.get(0))
        assertTemplateDefinition("name2", "a.yaml", "b.yaml", templateDefinitions.get(1))
    }

    @Test
    fun ignoreIfAsteriskInFromFile() {
        val templateDefinitions: List<TemplateDefinition> =
            parseArrowProperty("mc.template.[name1,name2]", "*", "dir")
        assertEquals(0, templateDefinitions.size)
    }

    private fun parseArrowProperty(
        key: String,
        from: String,
        to: String,
    ): List<TemplateDefinition> {
        val property: Property =
            property(
                key,
                tempDir.toString() + "/" + from + " -> " + tempDir.toString() + "/" + to,
                YAML,
                mock(DeclaringComponent::class.java),
            )
        return ArrayList(SquareBracketsNotationParser(defaultPattern()).parse(listOf(property)))
    }

    private fun assertTemplateDefinition(
        name: String,
        from: String,
        to: String,
        templateDefinition: TemplateDefinition,
    ) {
        assertEquals(name, templateDefinition.templateName)
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
