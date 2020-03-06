package io.microconfig.core.properties.resolver.placeholder;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolver.placeholder.PlaceholderBorder.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderBorderTest {
    @Test
    void test() {
        PlaceholderBorder borders = parse(new StringBuilder("hello ${rf} ${c1 ${c2 } @fsd}  ${app::component[dev]@value:${another}#{1+2}} ${}"));
        assertEquals("${app::component[dev]@value:${another}#{1+2}}", borders.toString());
    }
}