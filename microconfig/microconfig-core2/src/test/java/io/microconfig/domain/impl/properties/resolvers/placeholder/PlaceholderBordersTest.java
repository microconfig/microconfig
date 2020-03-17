package io.microconfig.domain.impl.properties.resolvers.placeholder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderBordersTest {
    @Test
    void test() {
        doTest("hello ${rf} ${c1 ${c2 } @fsd}  ${app::component[dev]@value:${another}#{1+2}} ${}", "${app::component[dev]@value:${another}#{1+2}}");
        doTest("${comp${c2@v2}onent[dev]@value:${another}#{1+2}}", "${c2@v2}");

        doTest("${component@value:${c2@v2}}", "${component@value:${c2@v2}}");
        doTest("${component@value:${c2@v2}", "${component@value:${c2@v2}");
        doTest("${component@value:${v}", "${component@value:${v}");
        doTest("${component@value${c2@v2}}", "${c2@v2}");
        doTest("${component@${c2@v2}}", "${c2@v2}");
        doTest("${${c1@v1}@${c2@v2}}", "${c1@v1}");
    }

    private void doTest(String line, String expected) {
        PlaceholderBorders border = PlaceholderBorders.findPlaceholderIn(line).orElseThrow(IllegalStateException::new);
        assertEquals(expected, border.toString());
    }
}