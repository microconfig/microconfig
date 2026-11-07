package io.microconfig.core.properties.templates

import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.templates.impl.StringTemplate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemplateGroupIndentationTest {
    @ParameterizedTest(name = "block: depth={0}, indent={1}, CRLF={2}")
    @MethodSource("indentations")
    fun preserveGroupBelowKeyAtDifferentDepths(depth: Int, indent: Int, crlf: Boolean) {
        val text = render(template(depth, indent, crlf, inline = false))
        assertNestedGroup(text, depth)
        assertTrue(" ".repeat(depth * indent) + "host: localhost" in text.lines(), text)
    }

    @ParameterizedTest(name = "inline: depth={0}, indent={1}, CRLF={2}")
    @MethodSource("indentations")
    fun inlineMultilineGroupRetainsLiteralPrefixAndIsNotValidYaml(depth: Int, indent: Int, crlf: Boolean) {
        val text = render(template(depth, indent, crlf, inline = true))
        assertTrue("database: host: localhost" in text, text)
        assertThrows(YAMLException::class.java) { Yaml().load<Any>(text) }
    }

    private fun render(content: String): String =
        StringTemplate(
            "t", File("source"), File("output"),
            TemplatePattern.defaultPattern().placeholderPattern, content
        )
            .resolveBy(
                Resolver { _, _, _ -> "host: localhost\nauth:\n  user: reader" },
                DeclaringComponentImpl("app", "component", "dev")
            )
            .content

    private fun template(depth: Int, indent: Int, crlf: Boolean, inline: Boolean): String {
        val newline = if (crlf) "\r\n" else "\n"
        val parents = (1 until depth).map { " ".repeat((it - 1) * indent) + "level$it:" }
        val keyIndent = " ".repeat((depth - 1) * indent)
        val value = if (inline) " \${common@db.*}"
        else newline + " ".repeat(depth * indent) + "\${common@db.*}"
        return (parents + (keyIndent + "database:" + value) + (keyIndent + "after: kept"))
            .joinToString(newline)
    }

    private fun assertNestedGroup(text: String, depth: Int) {
        var branch = Yaml().load<Map<*, *>>(text)
        for (level in 1 until depth) branch = branch["level$level"] as Map<*, *>
        val group = mapOf("host" to "localhost", "auth" to mapOf("user" to "reader"))
        assertEquals(mapOf("database" to group, "after" to "kept"), branch)
    }

    private fun indentations(): List<Arguments> =
        listOf(1, 2, 5, 10, 20).flatMap { depth ->
            listOf(2, 4).flatMap { indent ->
                listOf(false, true).map { crlf -> Arguments.of(depth, indent, crlf) }
            }
        }
}
