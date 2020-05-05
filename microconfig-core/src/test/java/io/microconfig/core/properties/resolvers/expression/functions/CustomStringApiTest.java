package io.microconfig.core.properties.resolvers.expression.functions;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolvers.expression.functions.CustomStringApi.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomStringApiTest {
    @Test
    void testFindGroup() {
        assertEquals("100m", findGroup("xmx(?<xmx>.+)", "xmx100m"));
        assertEquals("100", findGroup("\\d+", "xmx100m"));
        assertEquals("", findGroup("xmx", "some"));
        assertEquals("empty", findGroupOrDefault("xmx", "some", "empty"));
    }

    @Test
    void testBase64() {
        assertEquals("aGVsbG8=", base64("hello"));
    }

    @Test
    void testDelete() {
        assertEquals("hello", delete("h.e.l.l.o", "."));
    }

    @Test
    void substring() {
        assertEquals("e.l.l.o", substringAfterFirst("h.e.l.l.o", "."));
        assertEquals("o", substringAfterLast("h.e.l.l.o", "."));
    }
}