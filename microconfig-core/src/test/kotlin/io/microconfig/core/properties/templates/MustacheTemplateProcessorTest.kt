package io.microconfig.core.properties.templates

import io.microconfig.core.Microconfig
import io.microconfig.core.configtypes.ConfigTypeFilters.configType
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.templates.impl.MustacheTemplateProcessor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MustacheTemplateProcessorTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        Files.createDirectories(root.resolve("envs"))
        Files.writeString(root.resolve("envs/dev.yaml"), "portOffset: 1\n")
    }

    @Test
    fun renderYamlPathReferencedByPropertiesAsPlainText() {
        writeComponent("source", YAML, "path: C:\\temp\\new\n")
        writeComponent("target", PROPERTIES, "path=\${source@path}\n")
        assertEquals("C:\\temp\\new", render("target", "{{{path}}}"))
    }

    private fun writeComponent(component: String, format: ConfigFormat, content: String) {
        val file = root.resolve("components/$component/application${format.extension()}")
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun render(component: String, template: String): String {
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
        val properties =
            microconfig
                .environments()
                .getOrCreateByName("dev")
                .findComponentWithName(component)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .first()
        return MustacheTemplateProcessor()
            .process("mustache", File("template.mustache"), template, properties)
    }

    @Test
    fun decodePropertiesEscapesBeforeRendering() {
        writeComponent(
            "target",
            PROPERTIES,
            "path=C:\\\\temp\\\\new\nline=first\\nsecond\ncolor=\\#ff0000\nquoted=\\\"quoted\\\"\nunicode=\\u0041\n",
        )
        assertEquals(
            "C:\\temp\\new|first\nsecond|#ff0000|\"quoted\"|A",
            render("target", "{{{path}}}|{{{line}}}|{{{color}}}|{{{quoted}}}|{{unicode}}"),
        )
    }

    @Test
    fun preserveLiteralYamlBlockContents() {
        writeComponent("target", YAML, "code: |-\n  \"left\" + \"right\"\n")
        assertEquals("\"left\" + \"right\"", render("target", "{{{code}}}"))
    }

    @Test
    fun keepTemporaryPropertiesAvailableToTemplates() {
        writeComponent("target", PROPERTIES, "#var path=C:\\\\temp\\\\new\n")
        assertEquals("C:\\temp\\new", render("target", "{{{path}}}"))
    }

    @Test
    fun renderYamlTemporaryPropertiesAsPlainText() {
        writeComponent("target", YAML, "#var color: #ff0000\n")
        assertEquals("#ff0000", render("target", "{{{color}}}"))
    }
}
