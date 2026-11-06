package io.microconfig.core.properties.io.yaml

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.ClasspathReader.Companion.read
import io.microconfig.core.properties.io.selector.ConfigIoFactory
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class YamlTreeTest {
    private val tree = YamlTree.standard()

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

    private fun parse(text: String): Map<String, Any?> = Yaml().load(text)
}
