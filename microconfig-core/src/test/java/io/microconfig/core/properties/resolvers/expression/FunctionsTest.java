package io.microconfig.core.properties.resolvers.expression;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolvers.expression.functions.Functions.findGroup;
import static io.microconfig.core.properties.resolvers.expression.functions.Functions.findGroupOrDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionsTest {
    @Test
    void testFindGroup() {
        assertEquals("100m", findGroup("xmx(?<xmx>.+)", "xmx100m"));
        assertEquals("100", findGroup("\\d+", "xmx100m"));
        assertEquals("", findGroup("xmx", "some"));
        assertEquals("empty", findGroupOrDefault("xmx", "some", "empty"));
    }
}