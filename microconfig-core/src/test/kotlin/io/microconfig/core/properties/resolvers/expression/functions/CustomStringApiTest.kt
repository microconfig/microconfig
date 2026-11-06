package io.microconfig.core.properties.resolvers.expression.functions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CustomStringApiTest {
    @Test
    fun testFindGroup() {
        assertEquals("100m", findGroup("xmx(?<xmx>.+)", "xmx100m"))
        assertEquals("100", findGroup("\\d+", "xmx100m"))
        assertEquals("", findGroup("xmx", "some"))
        assertEquals("empty", findGroupOrDefault("xmx", "some", "empty"))
    }

    @Test
    fun testBase64() {
        assertEquals("aGVsbG8=", base64("hello"))
        assertEquals("aGVsbG8=", bytesBase64("hello".toByteArray()))
    }

    @Test
    fun removeUuidFromTheFunctionApi() {
        assertFalse(
            Class.forName(
                    "io.microconfig.core.properties.resolvers.expression.functions.CustomStringApiKt"
                )
                .methods
                .any { it.name == "uuid4" }
        )
    }

    @Test
    fun testDelete() {
        assertEquals("hello", delete("h.e.l.l.o", "."))
    }

    @Test
    fun substring() {
        assertEquals("e.l.l.o", substringAfterFirst("h.e.l.l.o", "."))
        assertEquals("o", substringAfterLast("h.e.l.l.o", "."))
    }
}
