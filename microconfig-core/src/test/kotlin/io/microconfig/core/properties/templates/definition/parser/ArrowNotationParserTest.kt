package io.microconfig.core.properties.templates.definition.parser

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplatePattern.Companion.defaultPattern
import io.microconfig.core.properties.templates.definition.parser.impl.ArrowNotationParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import java.io.File

class ArrowNotationParserTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun singleFile() {
        val templateDefinitions: List<TemplateDefinition> =
            parseArrowProperty("mc.template.name", "a.yaml", "b.yaml")
        assertTemplateDefinition("a.yaml", "b.yaml", templateDefinitions)
    }

    @Test
    fun multipleFiles() {
        createFile("first.yaml")
        createFile("second.yaml")
        val templateDefinitions: List<TemplateDefinition> =
            parseArrowProperty("mc.template.name", "*", "dir")
        assertEquals(2, templateDefinitions.size)
        assertTemplateDefinition("first.yaml", "dir/first.yaml", templateDefinitions)
        assertTemplateDefinition("second.yaml", "dir/second.yaml", templateDefinitions)
    }

    @Test
    fun nestedFolders() {
        createFile("first.yaml")
        File(tempDir, "sub/deeper").mkdirs()
        createFile("sub/second.yaml")
        createFile("sub/deeper/third.yaml")
        val templateDefinitions: List<TemplateDefinition> =
            parseArrowProperty("mc.template.name", "*", "dir")
        assertEquals(3, templateDefinitions.size)
        assertTemplateDefinition("first.yaml", "dir/first.yaml", templateDefinitions)
        assertTemplateDefinition("sub/second.yaml", "dir/sub/second.yaml", templateDefinitions)
        assertTemplateDefinition(
            "sub/deeper/third.yaml",
            "dir/sub/deeper/third.yaml",
            templateDefinitions,
        )
    }

    @Test
    fun ifKeyContainsSquareBrackets_Ignore() {
        val templateDefinitions: Collection<TemplateDefinition> =
            parseArrowProperty("mc.template.[name]", "a.yaml", "b.yaml")
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
        return ArrayList(ArrowNotationParser(defaultPattern()).parse(listOf(property)))
    }

    private fun assertTemplateDefinition(
        from: String,
        to: String,
        templateDefinitions: List<TemplateDefinition>,
    ) {
        templateDefinitions
            .stream()
            .filter({ td -> td.templateName.equals("name") })
            .filter({ td ->
                (td.fromFile.toString())
                    .replace('\\', '/')
                    .equals((File(tempDir, from).toString()).replace('\\', '/'))
            })
            .filter({ td ->
                (td.toFile.toString())
                    .replace('\\', '/')
                    .equals((File(tempDir, to).toString()).replace('\\', '/'))
            })
            .findFirst()
            .orElseThrow({ AssertionError("Template " + from + "->" + to + " not found") })
    }

    private fun createFile(s: String) {
        File(tempDir.toString() + "/" + s).createNewFile()
    }
}
