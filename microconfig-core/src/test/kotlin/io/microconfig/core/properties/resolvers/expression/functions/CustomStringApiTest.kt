package io.microconfig.core.properties.resolvers.expression.functions

import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.base64
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.bytesBase64
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.delete
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.findGroup
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.findGroupOrDefault
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.removeLineSeparators
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.replaceLineSeparators
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.substringAfterFirst
import io.microconfig.core.properties.resolvers.expression.functions.impl.CustomStringApi.substringAfterLast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

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
        assertFalse(CustomStringApi::class.java.methods.any { it.name == "uuid4" })
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

    @ParameterizedTest
    @MethodSource("lineSeparatorCases")
    fun removeAndReplaceEveryLogicalLineSeparator(text: String, removed: String, replaced: String) {
        assertEquals(removed, removeLineSeparators(text))
        assertEquals(replaced, replaceLineSeparators(text, "|"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["\$1\\tail", "\\", "\${literal}", "\r\n"])
    fun insertReplacementLiterallyWithoutRescanningIt(replacement: String) {
        assertEquals(
            "first${replacement}second${replacement}",
            replaceLineSeparators("first\r\nsecond\r", replacement)
        )
    }

    companion object {
        @JvmStatic
        fun lineSeparatorCases(): List<Arguments> = listOf(
            Arguments.of("first\nsecond", "firstsecond", "first|second"),
            Arguments.of("first\r\nsecond", "firstsecond", "first|second"),
            Arguments.of("first\rsecond", "firstsecond", "first|second"),
            Arguments.of("\r\n", "", "|"),
            Arguments.of("\n\r\n\r", "", "|||"),
            Arguments.of("a\r\n\r\nb\rc\n", "abc", "a||b|c|"),
            Arguments.of("\r\nstart\nend\r", "startend", "|start|end|"),
            Arguments.of("", "", ""),
            Arguments.of(" \t😀 Привет\\n\$\\r ", " \t😀 Привет\\n\$\\r ", " \t😀 Привет\\n\$\\r "),
        )
    }
}
