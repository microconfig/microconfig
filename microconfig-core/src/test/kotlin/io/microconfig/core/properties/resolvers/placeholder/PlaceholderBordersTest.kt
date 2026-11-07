package io.microconfig.core.properties.resolvers.placeholder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PlaceholderBordersTest {
    @Test
    fun test() {
        doTest(
            "hello \${rf} \${c1 \${c2 } @fsd}  \${app::component[dev]@value:\${another}#{1+2}} \${}",
            "\${app::component[dev]@value:\${another}#{1+2}}",
        )
        doTest("\${comp\${c2@v2}onent[dev]@value:\${another}#{1+2}}", "\${c2@v2}")
        doTest("\${component@value:\${c2@v2}}", "\${component@value:\${c2@v2}}")
        doTest("\${component@value:\${c2@v2}", "\${component@value:\${c2@v2}")
        doTest("\${component@value:\${v}", "\${component@value:\${v}")
        doTest("\${component@value\${c2@v2}}", "\${c2@v2}")
        doTest("\${component@\${c2@v2}}", "\${c2@v2}")
        doTest("\${\${c1@v1}@\${c2@v2}}", "\${c1@v1}")
        doTest("\${@} \${ @ } \${c@} \${@k} \${c@v}", "\${c@v}")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "\${one@value:\${two@value:\${three@value}}}",
            "{\"text\": \"} {\", \"nested\": {\"key\": \"value\"}}",
            "{\"text\": \"escaped \\\" } quote\"}",
            "#{\"abc\\\".length()}",
            "#{#{1 + 2} + {'a': 3}['a']}",
            "#{'it''s } quoted'}",
            "#{'\${other@value:'a}b'}'}",
            "first\nsecond",
        ]
    )
    fun preserveNestedDefaultBoundaries(defaultValue: String) {
        val placeholder = "\${component@value:$defaultValue}"
        val text = "prefix $placeholder|\${next@value}"
        val borders = PlaceholderBorders.findPlaceholderIn(text).orElseThrow()
        assertEquals(7, borders.startIndex)
        assertEquals(7 + placeholder.length, borders.endIndex)
        assertEquals(7 + placeholder.length, PlaceholderBorders.endOfPlaceholder(text, 7))
        assertEquals(defaultValue, borders.toPlaceholder("app", "dev").defaultValue)
    }

    @Test
    fun preserveBoundariesAcrossMixedNestedDefaults() {
        val defaults = listOf(
            "", "plain", "http://host:8080/a?x=1&y=2", "C:\\temp\\new", "a$#[]()@,:=",
            "'a}b{c'", "\"a}b{c\"", "{\"text\": \"escaped \\\" } quote\"}",
            "#{'it''s } text'}", "#{\"abc\\\".length()}", "#{ {'a': {'b': 2}}['a']['b'] }",
        )
        var cases = defaults
        repeat(3) {
            cases = cases.flatMap { value ->
                listOf(
                    "\${inner@missing:$value}",
                    "#{'\${inner@missing:$value}'}",
                    "{\"text\": \"\${inner@missing:$value}\"}",
                )
            }
            cases.forEach(::preserveNestedDefaultBoundaries)
        }
    }

    @Test
    fun findEndOfDeeplyNestedDefaultsWithoutRecursion() {
        val text = "\${inner@missing:".repeat(5000) + "'} { $ # []'" + "}".repeat(5000)
        assertEquals(text.length, PlaceholderBorders.endOfPlaceholder(text, 0))
    }

    @Test
    fun rejectIncompleteOuterBoundary() {
        assertEquals(-1, PlaceholderBorders.endOfPlaceholder("\${outer@value:\${inner@value}", 0))
    }

    private fun doTest(line: String, expected: String) {
        val border: PlaceholderBorders =
            PlaceholderBorders.findPlaceholderIn(StringBuilder(line))
                .orElseThrow(::IllegalStateException)
        assertEquals(expected, border.toString())
    }
}
