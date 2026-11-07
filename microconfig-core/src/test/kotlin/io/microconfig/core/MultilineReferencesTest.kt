package io.microconfig.core

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultilineReferencesTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest(name = "{0}, YAML target={1}, value={2}")
    @MethodSource("configValues")
    fun preserveValuesAcrossReferencesAndFormats(style: String, yamlTarget: Boolean, expected: String) {
        writeSource(style, expected)
        writeTarget(yamlTarget)
        val result = readTarget(buildOutput(), yamlTarget)
        assertEquals(expected, result["copy"])
        assertEquals(expected, result["chain"])
        assertEquals("before $expected after", result["composed"])
        assertEquals(expected.length.toString(), result["length"].toString())
        val group = if (yamlTarget) result["group"] else Yaml().load<Any>(result["group"] as String)
        assertEquals(mapOf("text" to expected), group)
        if (yamlTarget) {
            assertEquals(expected, result["quoted"])
            assertEquals(listOf(expected, mapOf("text" to expected)), result["block"])
            assertEquals(listOf(expected, mapOf("text" to expected)), result["flow"])
        }
        assertEquals("kept", result["sibling"])
    }

    @ParameterizedTest(name = "raw template: {0}, value={1}")
    @MethodSource("textValues")
    fun preserveExactValueInWholeTextTemplate(style: String, expected: String) {
        writeSource(style, expected)
        write("components/app/application.yaml", "mc.template.t: \${this@configDir}/template.txt -> rendered.txt\n")
        write("components/app/template.txt", "\${source@value}")
        assertEquals(expected, Files.readString(buildOutput().resolve("app/rendered.txt")))
    }

    @ParameterizedTest(name = "file line endings: {0}, CRLF={1}")
    @MethodSource("sourceLineEndings")
    fun preserveLogicalNewlinesWithDifferentSourceFileLineEndings(style: String, crlf: Boolean) {
        val expected = "first\nsecond\n\n"
        writeSource(style, expected, crlf)
        writeTarget(true)
        assertEquals(expected, readTarget(buildOutput(), true)["copy"])
    }

    @ParameterizedTest(name = "nested interpolation: {0}, YAML target={1}")
    @MethodSource("formatPairs")
    fun resolveMultilineValuesInsideOtherMultilineValues(style: String, yamlTarget: Boolean) {
        write("components/leaf/application.yaml", "text: |-\n  left\n  right\n")
        writeSource(style, "begin\n\${leaf@text}\nend\n")
        writeTarget(yamlTarget)
        val result = readTarget(buildOutput(), yamlTarget)
        assertEquals("begin\nleft\nright\nend\n", result["copy"])
        assertEquals(result["copy"], result["chain"])
    }

    @ParameterizedTest(name = "properties bridge: {0}, YAML target={1}")
    @MethodSource("formatPairs")
    fun preserveValuesThroughPropertiesBridge(style: String, yamlTarget: Boolean) {
        val expected = "\nO'Reilly \\\\server\\share\n\"quoted\"\n\n"
        writeSource(style, expected)
        write(
            "components/bridge/application.properties",
            "value=\${source@value}\ngroup.text=\${this@value}\n"
        )
        writeTarget(yamlTarget, "bridge")
        val result = readTarget(buildOutput(), yamlTarget)
        assertEquals(expected, result["copy"])
        assertEquals(expected, result["chain"])
    }

    @ParameterizedTest(name = "defaults: {0}, YAML target={1}")
    @MethodSource("formatPairs")
    fun preserveMultilineDefaultsAndDoNotReplaceExistingEmptyValues(style: String, yamlTarget: Boolean) {
        val expected = "first\nO'Reilly \"book\"\n\n"
        writeSource(style, expected)
        val separator = if (yamlTarget) ": " else "="
        write(
            "components/app/application${if (yamlTarget) ".yaml" else ".properties"}",
            "nested$separator\${absent@value:\${source@value}}\n" +
                    "empty$separator\${source@empty:fallback}\nemptyDefault$separator\${absent@value:\${source@empty}}\n"
        )
        val result = readTarget(buildOutput(), yamlTarget)
        assertEquals(expected, result["nested"])
        assertEquals("", result["empty"])
        assertEquals("", result["emptyDefault"])
    }

    @ParameterizedTest(name = "mixed interpolation: {0}, YAML target={1}")
    @MethodSource("formatPairs")
    fun evaluateSpelAndPlaceholdersInsideMultilineValue(style: String, yamlTarget: Boolean) {
        write("components/leaf/application.yaml", "text: |-\n  left\n  right\n")
        writeSource(style, "begin\n#{1 + 2}\n\${leaf@text}\n#{'\${leaf@text}'.length()}\nend")
        writeTarget(yamlTarget)
        val result = readTarget(buildOutput(), yamlTarget)
        assertEquals("begin\n3\nleft\nright\n10\nend", result["copy"])
        assertEquals(result["copy"], result["chain"])
    }

    @ParameterizedTest(name = "multiple template values: {0}")
    @MethodSource("sourceStyles")
    fun alignSubsequentMultilineValuesAfterEarlierSubstitutions(style: String) {
        writeSource(style, "one\ntwo")
        write("components/app/application.yaml", "mc.template.t: \${this@configDir}/template.txt -> rendered.txt\n")
        write("components/app/template.txt", "  A \${source@value} B \${source@value} Z")
        val text = Files.readString(buildOutput().resolve("app/rendered.txt"))
        assertEquals("  A one\n    two B one\n          two Z", text)
    }

    @Test
    fun preserveCrLfThroughLegacyStringResolver() {
        val expected = "first\r\nsecond"
        writeSource("properties", expected)
        val context = fileSource(
            root.resolve("components/source/application.properties").toFile(),
            0, false, "app", "dev"
        )
        val result = Microconfig.searchConfigsIn(root.toFile()).resolver().resolve(
            "\${source@value}", context, DeclaringComponentImpl("app", "app", "dev")
        )
        assertEquals(expected, PropertiesText.unescape(result))
    }

    @ParameterizedTest
    @EnumSource(ValueFormat::class)
    fun preserveCrLfWhenWritingEveryValueRepresentation(format: ValueFormat) {
        val expected = "first\r\nsecond\nthird\rfourth"
        val value = when (format) {
            ValueFormat.TEXT -> expected
            ValueFormat.PROPERTIES -> "first\\r\\nsecond\\nthird\\rfourth"
            ValueFormat.YAML -> "\"first\\r\\nsecond\\nthird\\rfourth\""
        }
        val property = PropertyImpl.property("value", value, ConfigFormat.PROPERTIES, null, format)
        val serialized = configIo().writeTo(root.resolve("result.properties").toFile()).serialize(listOf(property))
        val result = Properties().apply { load(StringReader(serialized)) }.getProperty("value")
        assertEquals(expected, result)
    }

    @ParameterizedTest(name = "indented template: {0}, depth={1}")
    @MethodSource("templateDepths")
    fun preserveTextWhenInsertedInIndentedYamlBlock(style: String, depth: Int) {
        val expected = "first\n  second\n\nthird"
        writeSource(style, expected)
        write("components/app/application.yaml", "mc.template.t: \${this@configDir}/template.yaml -> rendered.yaml\n")
        write("components/app/template.yaml", blockTemplate(depth))
        val text = Files.readString(buildOutput().resolve("app/rendered.yaml"))
        var branch = Yaml().load<Map<*, *>>(text)
        for (level in 1 until depth) branch = branch["level$level"] as Map<*, *>
        assertEquals(expected, branch["value"])
        assertEquals("kept", branch["sibling"])
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun writeSource(style: String, value: String, crlf: Boolean = false) {
        val yaml = style != "properties"
        val content = if (yaml) yamlSource(style, value) else propertiesSource(value)
        write(
            "components/source/application${if (yaml) ".yaml" else ".properties"}",
            if (crlf) content.replace("\n", "\r\n") else content
        )
    }

    private fun yamlSource(style: String, value: String): String {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            defaultScalarStyle = when (style) {
                "literal" -> DumperOptions.ScalarStyle.LITERAL
                "folded" -> DumperOptions.ScalarStyle.FOLDED
                else -> DumperOptions.ScalarStyle.DOUBLE_QUOTED
            }
        }
        return Yaml(options).dump(
            mapOf(
                "value" to value, "empty" to "",
                "group" to mapOf("text" to "\${this@value}")
            )
        )
    }

    private fun propertiesSource(value: String): String {
        val output = StringWriter()
        Properties().apply {
            setProperty("value", value)
            setProperty("empty", "")
            setProperty("group.text", "\${this@value}")
            store(output, null)
        }
        return output.toString()
    }

    private fun writeTarget(yaml: Boolean, component: String = "source") {
        val separator = if (yaml) ": " else "="
        val reference = "\${$component@value}"
        val common = "copy$separator$reference\nchain$separator\${this@copy}\n" +
                "composed${separator}${if (yaml) "\"before $reference after\"" else "before $reference after"}\n" +
                "length$separator#{'$reference'.length()}\ngroup$separator\${$component@group.*}\nsibling${separator}kept\n"
        val collections = if (yaml) "quoted: \"$reference\"\nblock:\n  - $reference\n" +
                "  - text: $reference\nflow: [$reference, {text: $reference}]\n" else ""
        write("components/app/application${if (yaml) ".yaml" else ".properties"}", common + collections)
    }

    private fun buildOutput(): Path {
        val output = root.resolve("output")
        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())
        return output
    }

    private fun readTarget(output: Path, yaml: Boolean): Map<*, *> {
        val content = Files.readString(output.resolve("app/application${if (yaml) ".yaml" else ".properties"}"))
        return if (yaml) Yaml().load<Map<*, *>>(content)
        else Properties().apply { load(StringReader(content)) }
    }

    private fun blockTemplate(depth: Int): String {
        val parents = (1 until depth).map { "  ".repeat(it - 1) + "level$it:" }
        val keyIndent = "  ".repeat(depth - 1)
        return (parents + (keyIndent + "value: |-") + ("  ".repeat(depth) + "\${source@value}") +
                (keyIndent + "sibling: kept")).joinToString("\n") + "\n"
    }

    private fun configValues(): List<Arguments> = styles().flatMap { style ->
        listOf(true, false).flatMap { target -> values().map { Arguments.of(style, target, it) } }
    }

    private fun textValues(): List<Arguments> = styles().flatMap { style ->
        values().map { Arguments.of(style, it) }
    }

    private fun styles(): List<String> = listOf("properties", "quoted", "literal", "folded")

    private fun sourceStyles(): List<Arguments> = styles().map { Arguments.of(it) }

    private fun values(): List<String> = listOf(
        "", "\n", "first\nsecond", "first\nsecond\n", "\nfirst\n\nsecond\n\n",
        "  first\n    second\nthird", "first \nsecond\t\n \n",
        "C:\\temp\\new\nO'Reilly \"book\"\n# heading: [a, b] {x}",
        "- item\ntrue\n123\n\${UNRESOLVED}\n.var ordinary\n#var key: literal",
        "😀 first\n🐈 second\nПривет", "first\\\nsecond",
        "first\r\nsecond", "first\rsecond\nthird",
    )

    private fun sourceLineEndings(): List<Arguments> = styles().flatMap { style ->
        listOf(false, true).map { Arguments.of(style, it) }
    }

    private fun formatPairs(): List<Arguments> = styles().flatMap { style ->
        listOf(false, true).map { Arguments.of(style, it) }
    }

    private fun templateDepths(): List<Arguments> = styles().flatMap { style ->
        listOf(1, 5).map { Arguments.of(style, it) }
    }
}
