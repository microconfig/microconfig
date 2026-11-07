package io.microconfig.core.properties.templates

import io.microconfig.core.Microconfig
import io.microconfig.core.MicroconfigRunner
import io.microconfig.core.configtypes.ConfigTypeFilters.eachConfigType
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.templates.definition.parser.impl.ArrowNotationParser
import io.microconfig.core.properties.templates.definition.parser.impl.FromToNotationParser
import io.microconfig.core.properties.templates.definition.parser.impl.SquareBracketsNotationParser
import io.microconfig.core.properties.templates.impl.TemplatesService
import io.microconfig.utils.Os
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemplateDeclarationsTest {
    @TempDir
    lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/app/source.txt", "payload")
    }

    @ParameterizedTest
    @MethodSource("invalidDeclarations")
    fun reportMalformedArrowsBeforeWriting(
        format: ConfigFormat,
        name: String,
        value: String,
        problem: String,
    ) {
        val fileName = "application${format.extension()}"
        val previous = destination.resolve("app/$fileName")
        Files.createDirectories(previous.parent)
        Files.writeString(previous, "previous config")
        val diff = previous.resolveSibling("diff-$fileName")
        Files.writeString(diff, "previous diff")
        write(
            "components/app/$fileName",
            declaration(format, "value", "new") +
                    declaration(format, "mc.file.valid", "\${this@configDir}/source.txt -> valid.txt") +
                    declaration(format, "mc.file.$name", value),
        )

        val error = assertThrows(IllegalArgumentException::class.java) { build() }

        assertTrue(error.message.orEmpty().contains(problem), error.message)
        assertTrue(error.message.orEmpty().contains("mc.file.$name"), error.message)
        assertTrue(error.message.orEmpty().contains("$fileName:3"), error.message)
        assertEquals("previous config", Files.readString(previous))
        assertEquals("previous diff", Files.readString(diff))
        assertFalse(Files.exists(destination.resolve("app/valid.txt")))
    }

    @ParameterizedTest
    @MethodSource("namedDeclarations")
    fun allowTemplateNamesMatchingNotationFields(
        format: ConfigFormat,
        notation: String,
        name: String,
    ) {
        val source = "\${this@configDir}/source.txt"
        val definitions =
            when (notation) {
                "fromTo" ->
                    declaration(format, "mc.file.$name.fromFile", source) +
                            declaration(format, "mc.file.$name.toFile", "rendered.txt")

                "arrow" -> declaration(format, "mc.file.$name", "$source -> rendered.txt")
                else -> declaration(format, "mc.file.[$name]", "$source -> rendered.txt")
            }
        write("components/app/application${format.extension()}", definitions)

        build()

        assertEquals("payload", Files.readString(destination.resolve("app/rendered.txt")))
    }

    @ParameterizedTest
    @MethodSource("fromToPaths")
    fun preserveArrowsInsideFromToPaths(
        format: ConfigFormat,
        name: String,
        explicitTarget: Boolean,
    ) {
        assumeFalse(Os.isWindows())
        write("components/app/source -> value.txt", "payload")
        write(
            "components/app/application${format.extension()}",
            declaration(
                format,
                "mc.file.$name.fromFile",
                "\${this@configDir}/source -> value.txt",
            ) +
                    if (explicitTarget)
                        declaration(format, "mc.file.$name.toFile", "rendered -> value.txt")
                    else "",
        )

        build()

        val target = if (explicitTarget) "rendered -> value.txt" else "source -> value.txt"
        assertEquals("payload", Files.readString(destination.resolve("app/$target")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun preserveNestedFromToKeys(extension: String) {
        val format = if (extension == "yaml") YAML else PROPERTIES
        write(
            "components/app/application.$extension",
            declaration(format, "mc.file.t.paths.fromFile", "\${this@configDir}/source.txt") +
                    declaration(format, "mc.file.t.paths.toFile", "rendered.txt"),
        )

        build()

        assertEquals("payload", Files.readString(destination.resolve("app/rendered.txt")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun preserveSpacesAdjacentToArrowSeparator(extension: String) {
        assumeFalse(Os.isWindows())
        val format = if (extension == "yaml") YAML else PROPERTIES
        write("components/app/source ", "payload")
        write(
            "components/app/application.$extension",
            declaration(format, "mc.file.t", "\${this@configDir}/source  ->  rendered.txt"),
        )

        build()

        assertEquals("payload", Files.readString(destination.resolve("app/ rendered.txt")))
    }

    @ParameterizedTest
    @MethodSource("literalArrowDestinations")
    fun preserveArrowTextInsideDestinationNames(
        format: ConfigFormat,
        name: String,
        target: String,
    ) {
        assumeFalse(Os.isWindows())
        val targetPath = if (name.startsWith('[')) "\${templateName}/$target" else target
        write(
            "components/app/application${format.extension()}",
            declaration(format, "mc.file.$name", "\${this@configDir}/source.txt -> $targetPath"),
        )

        build()

        val paths = if (name.startsWith('[')) listOf("one/$target", "two/$target") else listOf(target)
        paths.forEach { assertEquals("payload", Files.readString(destination.resolve("app/$it"))) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["custom", "[custom]"])
    fun leaveOtherNotationsForCustomParsers(name: String) {
        write(
            "components/app/application.yaml",
            "mc.file.$name: copy \${this@configDir}/source.txt\n",
        )
        val microconfig =
            Microconfig.searchConfigsIn(root.toFile()).withDestinationDir(destination.toFile())
        val properties =
            microconfig
                .inEnvironment("dev")
                .allComponents
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver())
                .first()
        val pattern = TemplatePattern.defaultPattern()
        val custom = TemplateDefinitionParser { values ->
            values
                .filter { it.value.startsWith("copy ") }
                .map { property ->
                    TemplateDefinition("file", "custom", pattern).apply {
                        setFromFile(property.value.removePrefix("copy "))
                        setToFile("rendered.txt")
                    }
                }
        }
        val service =
            TemplatesService(
                pattern,
                listOf(
                    FromToNotationParser(pattern),
                    ArrowNotationParser(pattern),
                    SquareBracketsNotationParser(pattern),
                    custom,
                ),
            )

        val template = service.resolveTemplates(properties, microconfig.resolver()).single()

        assertEquals("payload", template.content)
        assertEquals(destination.resolve("app/rendered.txt").toFile(), template.destination)
    }

    fun invalidDeclarations(): List<Arguments> =
        listOf(YAML, PROPERTIES).flatMap { format ->
            listOf("t", "[one,two]").flatMap { name ->
                listOf(
                    "\${this@configDir}/source.txt ->" to "Missing template destination path",
                    "-> rendered.txt" to "Missing template source path",
                    "->" to "Missing template source path",
                    "\${this@configDir}/source.txt -> one -> two" to
                            "Expected one ' -> ' separator",
                )
                    .map { (value, problem) -> Arguments.of(format, name, value, problem) }
            }
        }

    fun namedDeclarations(): List<Arguments> =
        listOf(YAML, PROPERTIES).flatMap { format ->
            listOf("fromTo", "arrow", "brackets").flatMap { notation ->
                listOf("fromFile", "toFile").map { name -> Arguments.of(format, notation, name) }
            }
        }

    fun fromToPaths(): List<Arguments> =
        listOf(YAML, PROPERTIES).flatMap { format ->
            listOf("fromFile", "toFile").flatMap { name ->
                listOf(false, true).map { explicitTarget ->
                    Arguments.of(format, name, explicitTarget)
                }
            }
        }

    fun literalArrowDestinations(): List<Arguments> =
        listOf(YAML, PROPERTIES).flatMap { format ->
            listOf("t", "[one,two]").flatMap { name ->
                listOf("->", "result ->").map { target -> Arguments.of(format, name, target) }
            }
        }

    private fun declaration(format: ConfigFormat, key: String, value: String): String =
        if (format == YAML) "$key: ${YamlScalars.doubleQuoted(value)}\n" else "$key=$value\n"

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build() =
        MicroconfigRunner(root.toFile(), destination.toFile())
            .build("dev", emptyList(), emptyList())
}
