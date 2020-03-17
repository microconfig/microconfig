package io.microconfig.core.properties.resolver.expression;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolver.expression.ExpressionFunctions.findGroup;
import static io.microconfig.core.properties.resolver.expression.ExpressionFunctions.findGroupOrDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionFunctionsTest {
    @Test
    void testFindGroup() {
        assertEquals("100m", findGroup("xmx(?<xmx>.+)", "xmx100m"));
        assertEquals("100", findGroup("\\d+", "xmx100m"));
        assertEquals("", findGroup("xmx", "some"));
        assertEquals("empty", findGroupOrDefault("xmx", "some", "empty"));
    }
}