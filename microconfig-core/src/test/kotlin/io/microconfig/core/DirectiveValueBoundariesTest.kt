package io.microconfig.core

import io.microconfig.core.configtypes.ConfigTypeFilters.configType
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.serializers.PropertySerializers.asString
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectiveValueBoundariesTest {
    @TempDir
    lateinit var root: Path

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("yamlScalarHeaders")
    fun keepDirectivesInsideYamlSequenceScalars(marker: String, key: String) {
        val header = if (key.isEmpty()) "  - $marker" else "  - $key: $marker"
        val indent = if (key.isEmpty()) "    " else "      "
        val content =
            "items:\n$header\n" +
                    indent +
                    ".var host: literal\n" +
                    indent +
                    ".var without-delimiter\n" +
                    indent +
                    "#var legacy: literal\n" +
                    indent +
                    "#include missing-literal-component\n" +
                    indent +
                    "#@Ignore\n" +
                    indent +
                    "echo done\n.var outside: real\n" +
                    "result: \${this@host:none}\nactual: \${this@outside:none}\n"
        val properties = build(content, YAML)
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        val item = (output(properties)["items"] as List<*>).first()
        val text = (if (key.isEmpty()) item else (item as Map<*, *>).values.first()) as String
        listOf(
            ".var host: literal",
            ".var without-delimiter",
            "#include missing-literal-component",
            "#@Ignore",
            "echo done",
        )
            .forEach { assertTrue(text.contains(it)) }
    }

    private fun build(
        content: String,
        format: ConfigFormat,
        environment: String = "dev",
    ): TypedProperties {
        Files.createDirectories(root.resolve("envs"))
        val component = Files.createDirectories(root.resolve("components/app"))
        Files.writeString(component.resolve("application${format.extension()}"), content)
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
        return microconfig
            .environments()
            .getOrCreateByName(environment)
            .findComponentWithName("app")
            .getPropertiesFor(configType(APPLICATION))
            .resolveBy(microconfig.resolver())
            .first()
    }

    private fun output(properties: TypedProperties): Map<String, Any?> =
        Yaml().load(properties.save(asString()))

    @ParameterizedTest
    @ValueSource(strings = ["\"a: | # quoted text\"", "'a: | # quoted text'", "prefix |"])
    fun keepRealDirectivesAfterTextThatIsNotAScalarHeader(text: String) {
        val content =
            "items:\n  - text: $text\n      .var outside: real\nresult: \${this@outside:none}\n"
        assertEquals("real", build(content, YAML).propertiesAsKeyValue["result"])
    }

    @Test
    fun keepPlaceholderDefaultsOutOfScalarHeaderParsing() {
        val content =
            "text: \${this@missing: | # default text}\n  .var outside: real\nresult: \${this@outside:none}\n"
        assertEquals("real", build(content, YAML).propertiesAsKeyValue["result"])
    }

    @Test
    fun detectTheEndOfAnAutoIndentedLiteral() {
        val content =
            "template: |\n    .var host: literal\n    echo done\n  .var outside: real\n" +
                    "result: \${this@host:none}\nactual: \${this@outside:none}\n"
        val properties = build(content, YAML)
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        assertEquals(".var host: literal\necho done\n", properties.propertiesAsKeyValue["template"])
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3])
    fun respectExplicitScalarIndentation(indentation: Int) {
        listOf(false, true).forEach { mapping ->
            val header = if (mapping) "  - text: |" else "  - |"
            val indent = " ".repeat((if (mapping) 4 else 2) + indentation)
            val content =
                "items:\n$header$indentation\n$indent.var host: literal\n${indent}echo done\nresult: \${this@host:none}\n"
            val properties = build(content, YAML)
            assertEquals("none", properties.propertiesAsKeyValue["result"])
            assertTrue(properties.save(asString()).contains(".var host: literal"))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["|", "|-", "|+", ">", ">-", ">+"])
    fun readMappingScalarsUsingTheSameHeadersAsSequenceScalars(header: String) {
        val content =
            "template: $header\n  .var host: literal\n  #var legacy: literal\n  echo done\n.var outside: real\n" +
                    "result: \${this@host:none}\nactual: \${this@outside:none}\n"
        val properties = build(content, YAML)
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        val text = output(properties)["template"] as String
        listOf(".var host: literal", "#var legacy: literal", "echo done").forEach {
            assertTrue(text.contains(it))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["&script |", "&script !!str >-"])
    fun preserveStandaloneAnchorsAndTags(header: String) {
        val content = "template: $header\n  literal\ncopy: *script\n"
        assertEquals(Yaml().load<Any>(content), output(build(content, YAML)))
    }

    @Test
    fun preserveExplicitStringTag() {
        val content = "template: !!str |\n  literal\nnumber: !!str 123\n"
        assertEquals(Yaml().load<Any>(content), output(build(content, YAML)))
    }

    @Test
    fun rejectUndefinedAliasesWithTheirSourceLocation() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                build("template: !!str |\n  literal\ncopy: *script\n", YAML)
            }
        assertTrue(error.message!!.contains("application.yaml:3"))
        assertTrue(error.message!!.contains("alias"))
    }

    @Test
    fun readScalarHeadersWithLongCommentsAndUnicodeSeparators() {
        val content =
            "template: | #" +
                    " ".repeat(10000) +
                    "\u2028\n  .var host: literal\n  echo done\nresult: \${this@host:none}\n"
        assertEquals("none", build(content, YAML).propertiesAsKeyValue["result"])
    }

    @ParameterizedTest(name = "{2}, env block={3}: {0}")
    @MethodSource("flowCollections")
    fun keepDirectivesOutOfFlowCollectionValues(
        value: String,
        expected: Any,
        directive: String,
        envBlock: Boolean,
    ) {
        val block = "section:\n  values:\n" + indent(value, 4) + "  $directive host: local\n"
        val content =
            "answer: \${this@host:none}\n" + if (envBlock) "+dev:\n" + indent(block, 2) else block
        val properties = build(content, YAML)
        assertEquals("local", properties.propertiesAsKeyValue["answer"])
        assertEquals(expected, Yaml().load<Any>(properties.propertiesAsKeyValue["section.values"]))
        if (envBlock)
            assertEquals("none", build(content, YAML, "prod").propertiesAsKeyValue["answer"])
    }

    private fun indent(text: String, width: Int): String =
        text.trimEnd('\n').lines().joinToString("\n", postfix = "\n") { " ".repeat(width) + it }

    @ParameterizedTest(name = "{0}, quote={1}, env block={2}")
    @MethodSource("quotedValueLayouts")
    fun keepDirectivesInsideMultilineQuotedValues(
        layout: String,
        quote: String,
        envBlock: Boolean,
    ) {
        val escaped = if (quote == "\"") "escaped \\\"quote\\\"" else "it''s quoted"
        val scalar =
            quote +
                    "😀first\u2028continued\n$escaped\n.var host: literal\n.var without-delimiter\n#var legacy: literal\n" +
                    "#include missing-quoted-component\n#@Ignore\n.var tail: closing" +
                    quote
        val node =
            when (layout) {
                "flow sequence" -> "[" + scalar.replace("\n", "\n  ") + "]"
                "block sequence" -> "- " + scalar.replace("\n", "\n  ")
                "mapping in sequence" -> "- message: " + scalar.replace("\n", "\n    ")
                else -> "{message: " + scalar.replace("\n", "\n  ") + "}"
            }
        val block = "text:\n" + indent(node, 2) + ".var outside: real\n"
        val content =
            "result: \${this@host:none}\ntail: \${this@tail:none}\nlegacy: \${this@legacy:none}\nactual: \${this@outside:none}\n" +
                    if (envBlock) ".var outside: base\n+dev:\n" + indent(block, 2) else block
        val properties = build(content, YAML)
        listOf("result", "tail", "legacy").forEach {
            assertEquals("none", properties.propertiesAsKeyValue[it])
        }
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        val parsed = Yaml().load<Any>(properties.propertiesAsKeyValue["text"])
        val item = if (parsed is Map<*, *>) parsed else (parsed as List<*>).first()
        val text = (if (item is Map<*, *>) item["message"] else item) as String
        listOf(
            ".var host: literal",
            ".var without-delimiter",
            "#include missing-quoted-component",
            "#@Ignore",
            ".var tail: closing",
        )
            .forEach { assertTrue(text.contains(it)) }
        if (envBlock) {
            val production = build(content, YAML, "prod").propertiesAsKeyValue
            assertEquals("none", production["result"])
            assertEquals("base", production["actual"])
        }
    }

    @ParameterizedTest(name = "quote={0}, indent={1}, text={2}, env block={3}")
    @MethodSource("dedentedQuotedLines")
    fun preserveDedentedLinesInsideQuotedListValues(
        quote: String,
        indentation: Int,
        text: String,
        envBlock: Boolean,
    ) {
        val prefix = " ".repeat(indentation)
        val block =
            "text:\n  - ${quote}first\n$prefix$text\n${prefix}last$quote\n.var outside: real\n"
        val content =
            "result: \${this@host:none}\nactual: \${this@outside:none}\n" +
                    if (envBlock) "+dev:\n" + indent(block, 2) else block
        val input = Yaml().load<Map<String, Any?>>(block)
        val properties = build(content, YAML)
        assertEquals(input["text"], output(properties)["text"])
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
    }

    @ParameterizedTest(name = "quote={0}, env block={1}, text={2}")
    @MethodSource("multilineQuotedProperties")
    fun readMultilineQuotedPropertiesCompletely(quote: String, envBlock: Boolean, text: String) {
        val block = "text: $quote$text$quote\n.var outside: real\n"
        val content =
            "result: \${this@host:none}\nactual: \${this@outside:none}\n" +
                    if (envBlock) "+dev:\n" + indent(block, 2) else block
        val input = Yaml().load<Map<String, Any?>>(block.replace("\${this@missing:tail}", "tail"))
        val properties = build(content, YAML)
        assertEquals(input["text"], output(properties)["text"])
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
    }

    @Test
    fun preserveQuotedPropertyKeysAndAnchorsWithMultilineStrings() {
        val content = "\"message:key\": &message \"first\n  second\"\nsame: *message\n"
        assertEquals(Yaml().load<Any>(content), output(build(content, YAML)))
    }

    @Test
    fun keepQuotesInsideBlockScalarsSeparateFromQuotedScalars() {
        val content =
            "items:\n  - |\n    \"unclosed literal quote\n    .var host: literal\n" +
                    "  - \"next\n    fake: |\n      literal text\n    .var quoted: literal\n    last\"\n" +
                    ".var outside: real\nresult: \${this@host:none}\nquoted: \${this@quoted:none}\nactual: \${this@outside:none}\n"
        val properties = build(content, YAML)
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("none", properties.propertiesAsKeyValue["quoted"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        val items = output(properties)["items"] as List<*>
        assertTrue((items[0] as String).contains("\"unclosed literal quote\n.var host: literal"))
        assertTrue((items[1] as String).contains(".var quoted: literal"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "- "])
    fun keepScalarHeaderLookalikesInsideFlowQuotes(sequence: String) {
        val content =
            "text:\n  $sequence[\"first: |\n      .var host: literal\n      last\"]\n" +
                    ".var outside: real\nresult: \${this@host:none}\nactual: \${this@outside:none}\n"
        val properties = build(content, YAML)
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        val values = Yaml().load<List<*>>(properties.propertiesAsKeyValue["text"])
        assertEquals(
            "first: | .var host: literal last",
            if (sequence.isEmpty()) values[0] else (values[0] as List<*>)[0],
        )
    }

    @Test
    fun keepQuotedDirectivesAfterUnresolvedFlowPlaceholders() {
        val content =
            "text:\n  [\${this@missing:\${this@alsoMissing:first}}, \"next\n" +
                    "    .var host: literal\n    #include missing-quoted-component\n    last\"]\n" +
                    ".var outside: real\nresult: \${this@host:none}\nactual: \${this@outside:none}\n"
        val properties = build(content, YAML)
        assertEquals("none", properties.propertiesAsKeyValue["result"])
        assertEquals("real", properties.propertiesAsKeyValue["actual"])
        assertEquals(
            listOf("first", "next .var host: literal #include missing-quoted-component last"),
            Yaml().load<Any>(properties.propertiesAsKeyValue["text"]),
        )
    }

    @Test
    fun keepOneLineFlowCollectionsAsCollectionsAfterDirectives() {
        val result =
            output(
                build(
                    "section:\n  values:\n    [one, two]\n  .var host: local\nanswer: \${this@host:none}\n",
                    YAML,
                )
            )
        assertEquals(listOf("one", "two"), (result["section"] as Map<*, *>)["values"])
        assertEquals("local", result["answer"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["[one,\n  two]", "{one: 1,\n  two: 2}"])
    fun keepMultilineScalarTextDistinctFromFlowCollections(text: String) {
        assertInstanceOf(
            String::class.java,
            output(build("text: |-\n" + indent(text, 2), YAML))["text"],
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                ".var:\n  .var:\n",
                ".var:\n  .var:\n    x: 1\n",
                "+dev:\n  .var:\n    .var:\n",
                "datasource:\n  .var:\n",
            ]
    )
    fun rejectNestedVarBlocksEvenWhenEmpty(content: String) {
        val error = assertThrows(IllegalArgumentException::class.java) { build(content, YAML) }
        assertTrue(error.message!!.contains("must be at the top level"))
    }

    @Test
    fun allowEmptyVarBlocksAtTheirSupportedLevels() {
        assertEquals(
            mapOf("a" to "1"),
            build("a: 1\n.var:\n+dev:\n  .var:\n", YAML).propertiesAsKeyValue,
        )
    }

    @ParameterizedTest(name = "{0} backslashes, {2}, continuation={3}")
    @MethodSource("propertiesContinuations")
    fun distinguishEscapedBackslashesFromPropertiesContinuations(
        slashes: Int,
        continued: Boolean,
        directive: String,
        alreadyContinued: Boolean,
    ) {
        val prefix = if (alreadyContinued) "path=first\\\n  next" else "path=C:"
        val content =
            prefix + "\\".repeat(slashes) + "\n$directive host=outside\nresult=\${this@host:none}\n"
        val properties = build(content, PROPERTIES)
        assertEquals(
            if (continued) "none" else "outside",
            properties.propertiesAsKeyValue["result"],
        )
        val path =
            (if (alreadyContinued) "first\\${LINES_SEPARATOR}next" else "C:") +
                    "\\".repeat(slashes) +
                    if (continued) LINES_SEPARATOR + directive + " host=outside" else ""
        assertEquals(path, properties.propertiesAsKeyValue["path"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["first\\  \n", "first\\\n\n", "first\\\n   \n"])
    fun terminatePropertiesValuesBeforeTheNextRealDirective(value: String) {
        val content = "path=$value.var host=outside\nresult=\${this@host:none}\n"
        val expected = Properties().apply { load(StringReader(content)) }
        val properties = build(content, PROPERTIES)
        assertEquals("outside", properties.propertiesAsKeyValue["result"])
        val actual = Properties().apply { load(StringReader(properties.save(asString()))) }
        assertEquals(expected.getProperty("path"), actual.getProperty("path"))
        assertEquals("outside", actual.getProperty("result"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["path=first\\", "path=first\\\\\\", "path=first\\\n  last\\"])
    fun finishPropertiesContinuationsAtEndOfFile(content: String) {
        val expected = Properties().apply { load(StringReader(content)) }
        val actual =
            Properties().apply { load(StringReader(build(content, PROPERTIES).save(asString()))) }
        assertEquals(expected.getProperty("path"), actual.getProperty("path"))
    }

    fun yamlScalarHeaders(): List<Arguments> =
        listOf(
            "|",
            "|-",
            "|+",
            ">",
            ">-",
            ">+",
            "|2",
            "|2-",
            "|-2",
            ">2+",
            ">+2",
            "| # literal",
            "&script |",
            "!!str |",
            "&script !!str |",
            "!!str &script >-",
        )
            .flatMap { marker ->
                listOf("", "text", "\"text:key\"", "&key text", "!!str text").map { key ->
                    Arguments.of(marker, key)
                }
            }

    fun flowCollections(): List<Arguments> =
        listOf(
            "[one, two]" to listOf("one", "two"),
            "[one,\n  two]" to listOf("one", "two"),
            "[one,\n# comment\n  two]" to listOf("one", "two"),
            "[it's, okay]" to listOf("it's", "okay"),
            "[\"😀😀😀😀\", two]" to listOf("😀😀😀😀", "two"),
            "[\"first\n  .var literalOnly: text\n  last\"]" to
                    listOf("first .var literalOnly: text last"),
            "{one: 1, two: 2}" to mapOf("one" to 1, "two" to 2),
            "{one: 1,\n  two: 2}" to mapOf("one" to 1, "two" to 2),
            "[\"\${this@missing:one}\", # comment\n  two]" to listOf("one", "two"),
        )
            .flatMap { (value, expected) ->
                listOf(".var", "#var").flatMap { directive ->
                    listOf(false, true).map { envBlock ->
                        Arguments.of(value, expected, directive, envBlock)
                    }
                }
            }

    fun quotedValueLayouts(): List<Arguments> =
        listOf("flow sequence", "block sequence", "mapping in sequence", "flow mapping").flatMap { layout ->
            listOf("\"", "'").flatMap { quote ->
                listOf(false, true).map { env -> Arguments.of(layout, quote, env) }
            }
        }

    fun dedentedQuotedLines(): List<Arguments> =
        listOf("\"", "'").flatMap { quote ->
            (0..2).flatMap { indentation ->
                listOf("#", ".var host: literal", "tail: literal").flatMap { text ->
                    listOf(false, true).map { env ->
                        Arguments.of(quote, indentation, text, env)
                    }
                }
            }
        }

    fun multilineQuotedProperties(): List<Arguments> =
        listOf("\"", "'").flatMap { quote ->
            listOf(false, true).flatMap { env ->
                listOf(
                    "first\n  .var host: literal",
                    "first\n  #include missing-quoted-component\n  #@Ignore\n  last",
                    "first\n\n  last: value",
                    "first\n  \${this@missing:tail}\n  last",
                    "😀first\u2028continued\n  " +
                            if (quote == "\"") "escaped \\\"quote\\\"" else "it''s quoted",
                )
                    .map { text -> Arguments.of(quote, env, text) }
            }
        }

    fun propertiesContinuations(): List<Arguments> =
        (0..6).flatMap { slashes ->
            listOf(".var", "#var").flatMap { directive ->
                listOf(false, true).map { continued ->
                    Arguments.of(slashes, slashes % 2 == 1, directive, continued)
                }
            }
        }
}
