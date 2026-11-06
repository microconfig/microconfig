package io.microconfig.core.properties.resolvers.placeholder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

    private fun doTest(line: String, expected: String) {
        val border: PlaceholderBorders =
            PlaceholderBorders.findPlaceholderIn(StringBuilder(line))
                .orElseThrow(::IllegalStateException)
        assertEquals(expected, border.toString())
    }
}
