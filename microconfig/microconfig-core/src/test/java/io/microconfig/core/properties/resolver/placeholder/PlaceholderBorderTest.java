package io.microconfig.core.properties.resolver.placeholder;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolver.placeholder.PlaceholderBorder.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderBorderTest {
    @Test
    void test() {
        doTest("hello ${rf} ${c1 ${c2 } @fsd}  ${app::component[dev]@value:${another}#{1+2}} ${}", "${app::component[dev]@value:${another}#{1+2}}");
        doTest("${app::comp${c2@v2}onent[dev]@value:${another}#{1+2}}", "${c2@v2}");
        doTest("${component@value:${c2@v2}}", "${component@value:${c2@v2}}");
    }

    private void doTest(String line, String expected) {
        assertEquals(expected, parse(new StringBuilder(line)).toString());
    }
}