package io.microconfig.core.properties.io.yaml

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.ClasspathReader.Companion.read
import io.microconfig.core.properties.io.selector.ConfigIoFactory
import io.microconfig.io.impl.DumpedFsReader
import io.microconfig.core.properties.io.yaml.impl.SnakeYaml
import java.io.StringReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions

class YamlTreeTest {
    private val tree = YamlTree.standard()

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun keepPlainMappingFormattingEquivalentToTheFullYamlEmitter(sectionBreaks: Boolean) {
        val writer = if (sectionBreaks) YamlTree.standard() else YamlTree.compact()
        val values = listOf("text", "0", "10", "true", "YES", "null", "1_000", "1.25",
            "010", ".inf", "-1", "'true'", "!!str true", "[one, two]", "{port: 80}",
            "&value text", "!custom value", "Привет")
        for (first in values.indices) for (second in values.indices) {
            val text = writer.toYaml(linkedMapOf("root" to values[first],
                "root.child" to "overlap", "section.first" to values[second],
                "section.nested.second" to "true", "z.child" to "0"))
            val node = Yaml(LoaderOptions().apply { isProcessComments = true }).compose(StringReader(text))
            val expected = if (sectionBreaks) SnakeYaml.serialize(node, true) else SnakeYaml.fragment(node)
            assertEquals(expected, text, "values=$first,$second, sections=$sectionBreaks")
        }
    }

    @Test
    fun preserveDotsInsideQuotedKeysAndBrackets() {
        val text =
            tree.toYaml(
                mapOf(
                    "metrics.distribution.percentiles-histogram[http.server.requests]" to "true",
                    "metadata.annotations.\"kubernetes.io/ingress.class\"" to "internal",
                    "a.b[c.d]" to "a",
                    "b.b]c.d[" to "a2",
                )
            )
        assertEquals(
            Yaml().load<Any>(read("configFormats/yaml/tree/escapedResult.yaml")),
            Yaml().load<Any>(text),
        )
    }

    @Test
    fun preserveSingleQuotedAndEscapedDoubleQuotedKeys() {
        assertEquals(
            mapOf("a.b" to "value", "a\".b" to "other"),
            parse(tree.toYaml(mapOf("'a.b'" to "value", "\"a\\\".b\"" to "other"))),
        )
    }

    @Test
    fun sortLeafValuesBeforeSectionsAndThenAlphabetically() {
        val text =
            tree.toYaml(
                mapOf(
                    "z.child" to "z",
                    "a.child" to "a",
                    "leaf.z" to "z",
                    "leaf" to "first",
                    "aaa" to "second",
                )
            )
        assertTrue(text.indexOf("aaa:") < text.indexOf("leaf:"))
        assertTrue(text.indexOf("leaf:") < text.indexOf("leaf.z:"))
        assertTrue(text.indexOf("leaf.z:") < text.indexOf("\na:"))
        assertTrue(text.indexOf("\na:") < text.indexOf("\nz:"))
    }

    @Test
    fun preserveScalarAndDescendantKeyConflicts() {
        assertEquals(
            mapOf(
                "tfs" to
                    mapOf("out" to "outV", "out.archiveDir" to "dirV", "out.shouldArchive" to true)
            ),
            parse(
                tree.toYaml(
                    mapOf(
                        "tfs.out.archiveDir" to "dirV",
                        "tfs.out" to "outV",
                        "tfs.out.shouldArchive" to "true",
                    )
                )
            ),
        )
    }

    @Test
    fun preserveExactMultilineTextAndLeadingWhitespace() {
        val values =
            mapOf(
                "indented" to "  a\nb",
                "afterEmptyLine" to "\n  a\nb",
                "plain" to "a\n  b",
                "crlf" to "a\r\nb\nc",
                "blankLines" to "\na\n\n\n",
            )
        assertEquals(
            values,
            parse(tree.toYaml(values.mapValues { YamlScalars.doubleQuoted(it.value) })),
        )
    }

    @Test
    fun preserveMultilineFlowCollections() {
        assertEquals(
            mapOf("list" to listOf("one", "two"), "map" to mapOf("one" to 1, "two" to 2)),
            parse(
                tree.toYaml(
                    mapOf("list" to "[\n one,\n two\n]\n", "map" to "{\n one: 1,\n two: 2\n}\n")
                )
            ),
        )
    }

    @Test
    fun roundTripNestedList() {
        val io = ConfigIoFactory.newConfigIo(DumpedFsReader())
        val original = classpathFile("configFormats/yaml/list/list.yaml")
        assertEquals(
            Yaml().load<Any>(original.readText()),
            parse(tree.toYaml(io.readFrom(original).propertiesAsMap())),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["text", "Привет", "a/b-c_d.txt", "true", "YES", "off", "null", "NULL",
        "0", "-10", "010", "0xA", "1_000", "1.25", ".5", ".inf", "-.Inf", ".NaN",
        "2001-12-15", "-", "---", "--- 'text'", "'true'", "!!str true",
        "[one, two]", "{key: value}"])
    fun preserveYamlTypesForPlainValuesAndSyntaxBoundaries(value: String) {
        assertEquals(mapOf("value" to Yaml().load<Any?>(value)),
            parse(tree.toYaml(mapOf("value" to value))))
    }

    @ParameterizedTest
    @ValueSource(strings = ["...", "*missing", "[unterminated"])
    fun rejectInvalidYamlFragments(value: String) {
        assertThrows(RuntimeException::class.java) { Yaml().load<Any?>(value) }
        assertThrows(RuntimeException::class.java) { tree.toYaml(mapOf("value" to value)) }
    }

    @Test
    fun keepRepeatedTypedAndStringKeysIndependentWithoutArtificialAliases() {
        val text = tree.toYaml(mapOf("first.true" to "true", "second.true" to "false",
            "first.'true'" to "text", "second.'true'" to "other"))

        assertEquals(mapOf("first" to mapOf(true to true, "true" to "text"),
            "second" to mapOf(true to false, "true" to "other")), parse(text))
        assertFalse(text.contains("&id"), text)
        assertFalse(text.contains("*id"), text)
    }

    @Test
    fun scopeYamlAnchorsToEachPropertyValue() {
        assertEquals(mapOf("first" to listOf("one"), "second" to listOf("two")),
            parse(tree.toYaml(mapOf("first" to "&item [one]", "second" to "&item [two]"))))
        assertThrows(RuntimeException::class.java) {
            tree.toYaml(mapOf("first" to "&item [one]", "second" to "*item"))
        }
    }

    private fun parse(text: String): Map<String, Any?> = Yaml().load(text)
}
