package io.microconfig.core

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.ResolveException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class PropertiesYamlBoundariesTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        Files.createDirectories(root.resolve("envs"))
        Files.writeString(root.resolve("envs/dev.yaml"), "{}\n")
    }

    @Test
    fun resolveGroupChildrenBeforeSerializingYaml() {
        writeComponent(
            "source",
            PROPERTIES,
            "db.value=#{\"abc\"}\n" +
                    "db.regex=#{#findGroup('\\\\d+', 'a123')}\n" +
                    "db.path=#{'C:\\\\temp'}\n",
        )
        writeComponent("target", PROPERTIES, "value=\${source@db.*}\n")

        val fragment = readProperties(build("target")).getProperty("value")

        assertEquals(
            mapOf("value" to "abc", "regex" to 123, "path" to "C:\\temp"),
            readYaml(fragment),
        )
    }

    @Test
    fun keepMultilineTextStartingWithDashInGroup() {
        writeComponent("source", PROPERTIES, "db.value=- first\\nsecond\n")
        writeComponent("target", PROPERTIES, "value=\${source@db.*}\n")

        val fragment = readProperties(build("target")).getProperty("value")

        assertEquals("- first\nsecond", readYaml(fragment)["value"])
    }

    @Test
    fun resolveGroupChildrenWithRootOverrides() {
        writeComponent("source", PROPERTIES, "setting=default\ndb.value=\${this@setting}\n")
        writeComponent("target", PROPERTIES, "setting=override\nvalue=\${source@db.*}\n")

        val fragment = readProperties(build("target")).getProperty("value")

        assertEquals("override", readYaml(fragment)["value"])
    }

    @Test
    fun detectCyclesThroughGroupChildren() {
        writeComponent("source", PROPERTIES, "db.value=\${source@db.*}\n")
        writeComponent("target", PROPERTIES, "value=\${source@db.*}\n")

        assertThrows(ResolveException::class.java) { build("target") }
    }

    @Test
    fun decodePropertiesExpressionBeforeEvaluation() {
        writeComponent(
            "source",
            PROPERTIES,
            "path=#{'C:\\\\temp'}\nregex=#{#findGroup('\\\\d+', 'a123')}\n",
        )

        val result = readProperties(build("source"))

        assertEquals("C:\\temp", result.getProperty("path"))
        assertEquals("123", result.getProperty("regex"))
    }

    @Test
    fun insertPropertiesTextNormallyInsideExpression() {
        writeComponent("source", YAML, "path: C:\\temp\\new\n")
        writeComponent("bridge", PROPERTIES, "path=\${source@path}\n")
        writeComponent(
            "target",
            PROPERTIES,
            "direct=#{'\${source@path}'.length()}\nviaBridge=#{'\${bridge@path}'.length()}\n",
        )

        val result = readProperties(build("target"))

        assertEquals("11", result.getProperty("direct"))
        assertEquals("11", result.getProperty("viaBridge"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["'", "\""])
    fun escapeQuotesInPropertiesExpressionArguments(quote: String) {
        val text = "O'Reilly \"book\" C:\\temp"
        writeComponent("source", YAML, "value: 'O''Reilly \"book\" C:\\temp'\n")
        writeComponent("bridge", PROPERTIES, "value=\${source@value}\n")
        writeComponent(
            "target",
            PROPERTIES,
            "direct=#{${quote}\${source@value}${quote}.length()}\n" +
                    "bridge=#{${quote}\${bridge@value}${quote}.length()}\n" +
                    "nested=#{#{${quote}\${source@value}${quote}.length()} + 1}\n",
        )

        val result = readProperties(build("target"))

        assertEquals(text.length.toString(), result.getProperty("direct"))
        assertEquals(text.length.toString(), result.getProperty("bridge"))
        assertEquals((text.length + 1).toString(), result.getProperty("nested"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["'abc'", "\"abc\""])
    fun keepLiteralQuotesWhenResolvingPropertiesValues(text: String) {
        writeComponent("source", YAML, "value: |\n  $text\n")
        writeComponent(
            "target",
            PROPERTIES,
            "plain=\${source@value}\nlength=#{'\${source@value}'.length()}\n",
        )

        val result = readProperties(build("target"))

        assertEquals("$text\n", result.getProperty("plain"))
        assertEquals((text.length + 1).toString(), result.getProperty("length"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["O\\u0027Reilly", "say \\u0022hello\\u0022", "literal \\\\u0027", "C:\\\\temp\\\\"])
    fun decodePropertiesArgumentsBeforeEscapingForExpressions(encoded: String) {
        writeComponent("source", PROPERTIES, "value=$encoded\n")
        writeComponent(
            "target",
            PROPERTIES,
            "single=#{'\${source@value}'.length()}\n" +
                    "double=#{\"\${source@value}\".length()}\n" +
                    "nested=#{#{'\${source@value}'.length()} + 1}\n" +
                    "plain=\${source@value}\n",
        )

        val expected = readProperties("value=$encoded").getProperty("value")
        val result = readProperties(build("target"))

        assertEquals(expected.length.toString(), result.getProperty("single"))
        assertEquals(expected.length.toString(), result.getProperty("double"))
        assertEquals((expected.length + 1).toString(), result.getProperty("nested"))
        assertEquals(expected, result.getProperty("plain"))
    }

    @Test
    fun evaluateDoubleQuotedExpressionArgumentsEndingWithBackslashInYaml() {
        writeComponent("source", YAML, "value: 'abc\\'\n")
        writeComponent(
            "target",
            YAML,
            "direct: #{\"abc\\\".length()}\n" +
                    "placeholder: #{\"\${source@value}\".length()}\n" +
                    "items: [{count: #{\"\${source@value}\".length()}}]\n",
        )

        assertEquals(
            mapOf("direct" to 4, "placeholder" to 4, "items" to listOf(mapOf("count" to 4))),
            readYaml(build("target")),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "#{\${missing@value:'abc'}.length()}",
                "#{\${source@value}.length()}",
                "#{'\${source@value}'.length()}",
                "#{#{\${source@value}.length()} + 0}",
            ]
    )
    fun evaluateYamlStringArgumentsBeforeSerializingExpressionResult(expression: String) {
        writeComponent("source", YAML, "value: 'abc'\n")
        writeComponent(
            "target",
            YAML,
            "value: " + expression + "\nitems: [{value: " + expression + "}]\n",
        )

        assertEquals(
            mapOf("value" to 3, "items" to listOf(mapOf("value" to 3))),
            readYaml(build("target")),
        )
    }

    @Test
    fun keepExpressionResultTypesIndependentOfTheirStringArguments() {
        writeComponent("source", YAML, "value: '123'\n")
        writeComponent(
            "target",
            YAML,
            "number: #{\${source@value}.length()}\n" +
                    "flag: #{\${source@value} == '123'}\n" +
                    "text: \"#{\${source@value}.length()}\"\n" +
                    "composed: prefix #{\${source@value}.length()} #{2 + 3}\n" +
                    "second: #{2 + 3} #{\${source@value}.length()}\n",
        )

        assertEquals(
            mapOf(
                "number" to 3,
                "flag" to true,
                "text" to "3",
                "composed" to "prefix 3 5",
                "second" to "5 3",
            ),
            readYaml(build("target")),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "#{\${source@value}.length()}",
                "#{'\${source@value}'.length()}",
                "#{\"\${source@value}\".length()}",
            ]
    )
    fun preserveQuoteCharactersInYamlExpressionArguments(expression: String) {
        writeComponent("source", YAML, "value: 'O''Reilly \"book\"'\n")
        writeComponent("target", YAML, "value: " + expression + "\n")

        assertEquals("O'Reilly \"book\"".length, readYaml(build("target"))["value"])
    }

    @Test
    fun ignoreYamlInlineCommentsAfterExpressions() {
        writeComponent("target", YAML, "value: #{2 + 3} # keep\n")

        assertEquals(5, readYaml(build("target"))["value"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc", "123", "true", "a: b"])
    fun resolvePlaceholdersProducedByExpressionsAsYamlValues(text: String) {
        writeComponent("source", YAML, "value: '" + text + "'\n")
        val expression = "#{'%c%csource@value%c'.formatted(36,123,125)}"
        writeComponent("target", YAML, "value: " + expression + "\nitems: [" + expression + "]\n")

        assertEquals(mapOf("value" to text, "items" to listOf(text)), readYaml(build("target")))
    }

    @Test
    fun keepYamlStringTypeAfterEmptyPrefix() {
        writeComponent("source", YAML, "value: '123'\n")
        writeComponent("target", YAML, "value: \${missing@value:}\${source@value}\n")

        assertEquals("123", readYaml(build("target"))["value"])
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "\\#ff0000",
                "b\\: c",
                "\\\"quoted\\\"",
                "null",
                "- first\\nsecond",
                "\\ leading",
                "C:\\\\temp",
            ]
    )
    fun quotePlainPropertiesTextInsertedIntoYaml(value: String) {
        writeComponent("source", PROPERTIES, "value=" + value + "\n")
        writeComponent("target", YAML, "value: \${source@value}\n")

        val result = readYaml(build("target"))["value"]

        assertEquals(readProperties("value=" + value).getProperty("value"), result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["\\#ff0000", "b\\: c", "\\\"quoted\\\""])
    fun quoteTheWholeComposedYamlScalar(value: String) {
        writeComponent("source", PROPERTIES, "value=" + value + "\n")
        writeComponent("target", YAML, "value: prefix \${source@value} suffix\n")

        val result = readYaml(build("target"))["value"]

        assertEquals(
            "prefix " + readProperties("value=" + value).getProperty("value") + " suffix",
            result,
        )
    }

    @Test
    fun preserveQuotesAndBackslashesInsideQuotedYamlScalar() {
        writeComponent("source", PROPERTIES, "value=\\\"C:\\\\temp\\\"\n")
        writeComponent("target", YAML, "value: \"prefix \${source@value} suffix\"\n")

        val result = readYaml(build("target"))["value"]

        assertEquals("prefix \"C:\\temp\" suffix", result)
    }

    @Test
    fun preserveQuotesProducedByExpressionInQuotedYaml() {
        writeComponent("target", YAML, "value: \"prefix #{'a' + '\\\"' + 'b'} suffix\"\n")

        assertEquals("prefix a\"b suffix", readYaml(build("target"))["value"])
    }

    @Test
    fun keepYamlTemporaryValuesAsPlainText() {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n")
        writeComponent("target", YAML, "#var alias: \${source@value}\nvalue: \${this@alias}\n")

        assertEquals("#ff0000", readYaml(build("target"))["value"])
    }

    @Test
    fun keepEmptyExpressionAndDefaultAsStrings() {
        writeComponent("target", YAML, "expression: #{''}\ndefault: \${missing@value:}\n")

        assertEquals(mapOf("expression" to "", "default" to ""), readYaml(build("target")))
    }

    @Test
    fun keepNativeYamlCollectionsAndQuotedStrings() {
        writeComponent("source", YAML, "list:\n  - one\n  - two\ntext: 'a: b'\n")
        writeComponent("target", YAML, "list: \${source@list}\ntext: \${source@text}\n")

        assertEquals(
            mapOf("list" to listOf("one", "two"), "text" to "a: b"),
            readYaml(build("target")),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["123", "true", "null", "1.5", "yes", "010", "#tag", "a: b"])
    fun preserveTheTypeOfReferencedYamlStrings(value: String) {
        writeComponent("source", YAML, "value: '" + value + "'\n")
        writeComponent("target", YAML, "value: \${source@value}\n")

        assertEquals(value, readYaml(build("target"))["value"])
    }

    @Test
    fun keepReferencedYamlStringsInsideCollections() {
        writeComponent("source", YAML, "color: '#tag'\nnumber: '123'\nflag: 'true'\ntext: 'a: b'\n")
        writeComponent(
            "target",
            YAML,
            "block:\n  - \${source@color}\n  - \${source@number}\n  - \${source@flag}\n  - \${source@text}\n" +
                    "flow: [\${source@color}, \${source@number}, \${source@flag}, \${source@text}]\n",
        )

        val expected = listOf("#tag", "123", "true", "a: b")
        assertEquals(mapOf("block" to expected, "flow" to expected), readYaml(build("target")))
    }

    @Test
    fun keepQuotedStringTypeWhenComposedWithEmptyDefault() {
        writeComponent("source", YAML, "value: '123'\n")
        writeComponent("target", YAML, "value: \${source@value}\${missing@value:}\n")

        assertEquals("123", readYaml(build("target"))["value"])
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            ["\"quoted\"", "\"left\" + \"right\"", "'quoted'", "123", "#tag", "[a, b]", "- item"]
    )
    fun preserveLiteralBlockContentsInProperties(value: String) {
        writeComponent("source", YAML, "value: |-\n  " + value + "\n")
        writeComponent("target", PROPERTIES, "value=\${source@value}\n")

        assertEquals(value, readProperties(build("target")).getProperty("value"))
    }

    @Test
    fun preserveLiteralBlockContentsInYamlAndLists() {
        writeComponent("source", YAML, "code: |-\n  \"left\" + \"right\"\nnumber: |-\n  123\n")
        writeComponent(
            "target",
            YAML,
            "code: \${source@code}\nnumber: \${source@number}\nlist: [\${source@code}, \${source@number}]\n",
        )

        val code = "\"left\" + \"right\""
        assertEquals(
            mapOf("code" to code, "number" to "123", "list" to listOf(code, "123")),
            readYaml(build("target")),
        )
        assertEquals(mapOf("code" to code, "number" to "123"), readYaml(build("source")))
    }

    @Test
    fun keepLiteralBlockStyleInEnvironmentOverrides() {
        writeComponent("source", YAML, "+dev:\n  value: |-\n    \"quoted\"\n")
        writeComponent("target", PROPERTIES, "value=\${source@value}\n")

        assertEquals("\"quoted\"", readProperties(build("target")).getProperty("value"))
    }

    @Test
    fun preserveLiteralTextInsideGroupFragments() {
        writeComponent(
            "source",
            YAML,
            "db:\n  code: |-\n    \"left\" + \"right\"\n  number: |-\n    123\n",
        )
        writeComponent("target", PROPERTIES, "value=\${source@db.*}\n")

        val fragment = readProperties(build("target")).getProperty("value")

        assertEquals(mapOf("code" to "\"left\" + \"right\"", "number" to "123"), readYaml(fragment))
    }

    @Test
    fun distinguishLiteralQuotesFromYamlQuotesInProperties() {
        writeComponent("source", YAML, "quoted: '123'\nliteral: |-\n  \"123\"\n")
        writeComponent(
            "target",
            PROPERTIES,
            "quoted=\${source@quoted}\nliteral=\${source@literal}\n",
        )

        val result = readProperties(build("target"))

        assertEquals("123", result.getProperty("quoted"))
        assertEquals("\"123\"", result.getProperty("literal"))
    }

    @Test
    fun preserveYamlStructuresInDefaultValues() {
        writeComponent(
            "target",
            YAML,
            "list: \${missing@value:[a, b]}\nmap: \${missing@value:{a: b}}\n" +
                    "text: \"\${missing@value:[a, b]}\"\nnumber: \${missing@value:'123'}\n",
        )

        assertEquals(
            mapOf(
                "list" to listOf("a", "b"),
                "map" to mapOf("a" to "b"),
                "text" to "[a, b]",
                "number" to "123",
            ),
            readYaml(build("target")),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "C:\\\\temp\\\\new",
                "\\\"quoted\\\"",
                "\\#ff0000",
                "b\\: c",
                "O'Reilly",
                "first\\nsecond",
                "123",
                "true",
            ]
    )
    fun preservePropertiesTextInQuotedCollectionElements(value: String) {
        writeComponent("source", PROPERTIES, "value=" + value + "\n")
        writeComponent(
            "target",
            YAML,
            "flow: [\"\${source@value}\"]\nblock:\n  - \"\${source@value}\"\n" +
                    "single: ['\${source@value}']\n",
        )

        val expected = readProperties("value=" + value).getProperty("value")
        assertEquals(
            mapOf(
                "flow" to listOf(expected),
                "block" to listOf(expected),
                "single" to listOf(expected),
            ),
            readYaml(build("target")),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["C:\\\\temp\\\\new", "\\\"quoted\\\"", "\\#ff0000", "b\\: c", "first\\nsecond"]
    )
    fun quotePropertiesTextInsideNestedCollections(value: String) {
        writeComponent("source", PROPERTIES, "value=" + value + "\n")
        writeComponent(
            "target",
            YAML,
            "flow: [{value: \${source@value}}]\nblock:\n  - value: \${source@value}\n",
        )

        val expected =
            listOf(mapOf("value" to readProperties("value=" + value).getProperty("value")))
        assertEquals(mapOf("flow" to expected, "block" to expected), readYaml(build("target")))
    }

    @Test
    fun resolveExpressionsAndPreserveNativeTypesInsideCollections() {
        writeComponent("source", PROPERTIES, "path=C:\\\\temp\\\\new\n")
        writeComponent(
            "target",
            YAML,
            "values: [#{2 * 3}, #{'\${source@path}'.length()}, true, null, '123']\n",
        )

        assertEquals(listOf(6, 11, true, null, "123"), readYaml(build("target"))["values"])
    }

    @Test
    fun preserveUnicodeOffsetsAndComposedScalarTextInCollections() {
        writeComponent("source", PROPERTIES, "path=C:\\\\temp\\\\new\ntext=\\#ff0000\n")
        writeComponent(
            "target",
            YAML,
            "values: [\"🙂 \${source@path}\", prefix \${source@text} suffix, {nested: \"🐈 \${source@text}\"}]\n",
        )

        assertEquals(
            listOf("🙂 C:\\temp\\new", "prefix #ff0000 suffix", mapOf("nested" to "🐈 #ff0000")),
            readYaml(build("target"))["values"],
        )
    }

    @Test
    fun preserveBlockScalarContentsInsideCollections() {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n")
        writeComponent(
            "target",
            YAML,
            "values:\n  - |-\n    first \${source@value}\n    second\n" +
                    "  - >-\n    first \${source@value}\n    second\n  - last\n",
        )

        assertEquals(
            listOf("first #ff0000\nsecond", "first #ff0000 second", "last"),
            readYaml(build("target"))["values"],
        )
    }

    @Test
    fun preserveExplicitBlockIndentationAndFollowingKeys() {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n")
        writeComponent(
            "target",
            YAML,
            "items:\n  - value: |2-\n      prefix \${source@value}\n    next: ok\n",
        )

        assertEquals(
            listOf(mapOf("value" to "prefix #ff0000", "next" to "ok")),
            readYaml(build("target"))["items"],
        )
    }

    @Test
    fun preserveEmojiInDefaultsAndResolvePlainBlockExpressions() {
        writeComponent("source", PROPERTIES, "path=C:\\\\temp\\\\new\n")
        writeComponent(
            "target",
            YAML,
            "values:\n  - \${missing@value:🐈}\n  - #{'\${source@path}'.length()}\n  - \"🙂 \${source@path}\"\n",
        )

        assertEquals(listOf("🐈", 11, "🙂 C:\\temp\\new"), readYaml(build("target"))["values"])
    }

    @Test
    fun preserveTagsAnchorsAndDynamicAliasesInCollections() {
        writeComponent("source", PROPERTIES, "name=ref\nvalue=123\n")
        writeComponent(
            "target",
            YAML,
            "values: [&\${source@name} !!str \${source@value}, *\${source@name}]\n",
        )

        assertEquals(listOf("123", "123"), readYaml(build("target"))["values"])
    }

    @Test
    fun resolveCollectionsReferencingAnchorsDeclaredOutsideThem() {
        writeComponent("source", PROPERTIES, "value=resolved\n")
        writeComponent(
            "target",
            YAML,
            "shared: &shared [base]\nvalues: [*shared, \${source@value}]\n" +
                    "spliced:\n  - *shared\n  \${this@shared}\n  - \${source@value}\n",
        )

        val result = readYaml(build("target"))
        assertEquals(listOf(listOf("base"), "resolved"), result["values"])
        assertEquals(listOf(listOf("base"), "base", "resolved"), result["spliced"])
    }

    @Test
    fun resolveLastBlockScalarWithoutTrailingLineBreak() {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n")
        writeComponent("target", YAML, "items:\n  - |-\n    \${source@value}")

        assertEquals(listOf("#ff0000"), readYaml(build("target"))["items"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " # keep"])
    fun preserveStandaloneYamlListSplices(suffix: String) {
        writeComponent("source", YAML, "items:\n  - second\n  - third\n")
        writeComponent("target", YAML, "items:\n  - first\n  \${source@items}" + suffix + "\n")

        assertEquals(listOf("first", "second", "third"), readYaml(build("target"))["items"])
    }

    @Test
    fun distinguishFlowElementsFromStandaloneListSplices() {
        writeComponent("text", PROPERTIES, "color=\\#ff0000\n")
        writeComponent("source", YAML, "items:\n  - second\n  - third\n")
        writeComponent(
            "target",
            YAML,
            "items:\n  - nested: [\n      \${text@color}\n    ]\n  \${source@items}\n",
        )

        assertEquals(
            listOf(mapOf("nested" to listOf("#ff0000")), "second", "third"),
            readYaml(build("target"))["items"],
        )
    }

    @Test
    fun preserveStandardYamlSyntaxAndValues() {
        val content = ClasspathReader.read("repo/components/snakeYamlSyntax/application.yaml")
        writeComponent("target", YAML, content)

        assertEquals(readYaml(content), readYaml(build("target")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["|", "|-", "|+", ">", ">-", ">+"])
    fun preserveExactScalarContentsAfterPlaceholderResolution(header: String) {
        val content = "value: $header\n\n  first \${source@text}\n\n  last\n\n\n"
        writeComponent("source", PROPERTIES, "text=resolved\n")
        writeComponent("target", YAML, content)

        assertEquals(
            readYaml(content.replace("\${source@text}", "resolved")),
            readYaml(build("target")),
        )
    }

    @Test
    fun preserveExplicitScalarTagsAfterPlaceholderResolution() {
        writeComponent("source", PROPERTIES, "number=12\nflag=true\n")
        writeComponent(
            "target",
            YAML,
            "number: !!int '\${source@number}'\nflag: !!bool '\${source@flag}'\ntext: !!str \${source@number}\n",
        )

        assertEquals(
            mapOf("number" to 12, "flag" to true, "text" to "12"),
            readYaml(build("target")),
        )
    }

    @Test
    fun preserveExplicitTagsOnBlockScalarsAndKeys() {
        writeComponent("source", PROPERTIES, "number=12\n")
        writeComponent(
            "target",
            YAML,
            "!!str 123: text\nnumber: !!int |-\n  \${source@number}\nitems:\n  - !!int |-\n    \${source@number}\n",
        )

        assertEquals(
            mapOf("123" to "text", "number" to 12, "items" to listOf(12)),
            readYaml(build("target")),
        )
    }

    private fun writeComponent(component: String, format: ConfigFormat, text: String) {
        val file = root.resolve("components/" + component + "/application" + format.extension())
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun build(component: String): String {
        Files.writeString(
            root.resolve("envs/dev.yaml"),
            "group:\n  components: [" + component + "]\n",
        )
        val destination = root.resolve("build")
        MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", listOf(), listOf())

        val yaml = destination.resolve(component + "/application.yaml")
        return Files.readString(
            if (Files.exists(yaml)) yaml
            else destination.resolve(component + "/application.properties")
        )
    }

    private fun readProperties(text: String): Properties {
        val properties = Properties()
        properties.load(StringReader(text))
        return properties
    }

    private fun readYaml(text: String): Map<*, *> {
        return Yaml().load(text)
    }
}
