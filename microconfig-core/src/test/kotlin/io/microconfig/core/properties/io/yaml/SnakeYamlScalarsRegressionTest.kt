package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.io.yaml.impl.SnakeYaml
import java.io.StringReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

class SnakeYamlScalarsRegressionTest {
    @ParameterizedTest
    @ValueSource(strings = ["text", "a/b-c_d.txt", "true", "YES", "off", "null", "NULL", "0",
        "-10", "010", "00", "0_1", "0xA", "1_000", "1.25", ".5", ".inf", "-.Inf", ".NaN",
        "2001-12-15", "Привет", "---text", "...text", "'true'", "!!str true", "!custom value",
        "&value text", "[one, two]", "{key: value}", "|-\n  first\n  second\n"])
    fun keepScalarParsingAndFragmentsEquivalentToTheFullYamlPipeline(value: String) {
        val expected = Yaml().compose(StringReader(value))
        val actual = SnakeYaml.parse(value)

        assertEquals(expected.tag, actual.tag)
        if (expected is ScalarNode) {
            assertEquals(expected.value, (actual as ScalarNode).value)
            assertEquals(expected.scalarStyle, actual.scalarStyle)
        }
        val serialized = SnakeYaml.serialize(expected)
        val block = expected is ScalarNode &&
            expected.scalarStyle in listOf(ScalarStyle.LITERAL, ScalarStyle.FOLDED)
        assertEquals(if (block) serialized else serialized.removeSuffix("\n"), SnakeYaml.fragment(actual))
    }

    @ParameterizedTest
    @ValueSource(strings = ["text", "true", "null", "10", "010", "---text", "Привет"])
    fun preserveExplicitTagsQuotedStylesAndAnchorsWhenRenderingScalars(value: String) {
        for (tag in listOf(Tag.STR, Tag.INT, Tag.BOOL, Tag.NULL, Tag("!custom"))) {
            for (style in listOf(ScalarStyle.PLAIN, ScalarStyle.SINGLE_QUOTED, ScalarStyle.DOUBLE_QUOTED)) {
                val node = ScalarNode(tag, value, null, null, style)
                assertEquals(SnakeYaml.serialize(node).removeSuffix("\n"), SnakeYaml.fragment(node))
                node.anchor = "original"
                assertEquals(SnakeYaml.serialize(node).removeSuffix("\n"), SnakeYaml.fragment(node))
            }
        }
    }
}
