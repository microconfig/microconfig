package io.microconfig.core.properties.resolvers.expression.functions;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.resolvers.expression.functions.CustomStringApi.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("aGVsbG8=", bytesBase64("hello".getBytes()));
    }

    @Test
    void testUuid4() {
        assertTrue(uuid4().matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));
        assertNotEquals(uuid4(), uuid4());
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