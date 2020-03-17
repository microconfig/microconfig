package io.microconfig.core.properties.resolver.expression;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolver.expression.ExpressionFunctions.findGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionFunctionsTest {
    @Test
    void testFindGroup() {
        assertEquals("", findGroup("xmx", "some"));
        assertEquals("100m", findGroup("xmx(?<xmx>.+)", "xmx100m"));
        assertEquals("100", findGroup("\\d+", "xmx100m"));
    }
}