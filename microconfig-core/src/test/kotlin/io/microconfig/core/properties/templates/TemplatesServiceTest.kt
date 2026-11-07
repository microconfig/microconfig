package io.microconfig.core.properties.templates

import io.microconfig.core.MicroconfigRunner
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.io.properties.PropertiesText.escape
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.templates.Template
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemplatesServiceTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        Files.createDirectories(root.resolve("envs"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [target]\n")
    }

    @ParameterizedTest
    @MethodSource("formatsAndNotations")
    fun decodeTemplatePathsBeforeParsing(format: ConfigFormat, notation: String, type: String) {
        val directory = if (format == YAML) "dir # files" else "dir\\u000Ciles"
        val source = root.resolve(directory).resolve("template.txt")
        Files.createDirectories(source.parent)
        Files.writeString(source, "hello")
        val sourcePath = "\${this@configRoot}/" + sourceText(directory, format) + "/template.txt"
        val destinationPath = "output " + sourceText(directory, format) + "/\${this@name}-\${templateName}.txt"
        writeComponent(
            format,
            templateDefinition(format, notation, type, sourcePath, destinationPath),
        )
        val templates = buildTemplates()
        assertEquals(if (notation == "brackets") 2 else 1, templates.size)
        val names = if (notation == "brackets") listOf("first", "second") else listOf("example")
        templates.zip(names).forEach { (template, name) ->
            val destination = destinationFile("output $directory/target-$name.txt").toFile()
            assertEquals(source.toFile().canonicalFile, template.source.canonicalFile)
            assertEquals(destination.canonicalFile, template.destination.canonicalFile)
            assertEquals("hello", template.content)
            assertEquals("hello", destination.readText())
        }
    }

    private fun sourceText(text: String, format: ConfigFormat): String =
        if (format == PROPERTIES) escape(text) else text

    private fun templateDefinition(
        format: ConfigFormat,
        notation: String,
        type: String,
        from: String,
        to: String,
    ): String {
        val prefix = "mc.$type."
        return when (notation) {
            "fromTo" ->
                declaration(format, prefix + "example.fromFile", from) +
                    declaration(format, prefix + "example.toFile", to)
            "arrow" -> declaration(format, prefix + "example", "$from -> $to")
            "brackets" -> declaration(format, prefix + "[first,second]", "$from -> $to")
            else -> throw IllegalArgumentException(notation)
        }
    }

    private fun declaration(format: ConfigFormat, key: String, value: String): String =
        if (format == YAML) "$key: ${YamlScalars.doubleQuoted(value)}\n" else "$key=$value\n"

    private fun writeComponent(format: ConfigFormat, content: String) =
        writeComponent("target", format, content)

    private fun writeComponent(component: String, format: ConfigFormat, content: String) {
        val file = root.resolve("components/$component/application${format.extension()}")
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun buildTemplates(): List<Template> {
        val runner = MicroconfigRunner(root.toFile(), root.resolve("build").toFile())
        val properties = runner.buildProperties("dev", listOf(), listOf())
        properties.save(runner.toFiles())
        return properties.save { _, templates, _, _, _ -> templates }.flatten()
    }

    private fun destinationFile(relativePath: String): Path =
        root.resolve("build/target").resolve(relativePath)

    @Test
    fun keepQuotesInLiteralYamlDestination() {
        Files.writeString(root.resolve("template.txt"), "hello")
        writeComponent(
            YAML,
            "mc.template.example.fromFile: \${this@configRoot}/template.txt\nmc.template.example.toFile: |-\n  'result.txt'\n",
        )
        assertEquals("'result.txt'", buildTemplates().first().fileName)
        assertEquals("hello", destinationFile("'result.txt'").toFile().readText())
    }

    @Test
    fun decodePropertiesPathEscapedDuringPlaceholderResolution() {
        val source = root.resolve("dir\\u000Ciles/template.txt")
        Files.createDirectories(source.parent)
        Files.writeString(source, "hello")
        writeComponent("pathSource", YAML, "path: |-\n  $source\n")
        writeComponent(
            PROPERTIES,
            "mc.template.example.fromFile=\${pathSource@path}\nmc.template.example.toFile=result.txt\n",
        )
        assertEquals(source.toFile().canonicalFile, buildTemplates().first().source.canonicalFile)
        assertEquals("hello", destinationFile("result.txt").toFile().readText())
    }

    fun formatsAndNotations(): List<Arguments> =
        listOf(YAML, PROPERTIES).flatMap { format ->
            listOf("fromTo", "arrow", "brackets").flatMap { notation ->
                listOf("template", "mustache", "file").map { type ->
                    Arguments.of(format, notation, type)
                }
            }
        }
}
