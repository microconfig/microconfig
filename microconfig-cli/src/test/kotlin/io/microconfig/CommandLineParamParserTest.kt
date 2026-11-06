package io.microconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CommandLineParamParserTest {
    @ParameterizedTest
    @ValueSource(strings = ["", " , , ", "\t\r\n"])
    fun omitEmptyListValues(value: String) {
        val params = CommandLineParamParser.parse("-envs", value)
        assertEquals(emptyList<String>(), params.listValue("envs"))
    }

    @Test
    fun trimAsciiWhitespaceAndPreserveUnicodeInListValues() {
        val params = CommandLineParamParser.parse("-envs", " first, ,\tsecond\t, \u00a0third, ")
        assertEquals(listOf("first", "second", "\u00a0third"), params.listValue("envs"))
    }

    @Test
    fun parse() {
        val params: CommandLineParamParser =
            CommandLineParamParser.parse(
                "-r",
                "/home/repo",
                "-d",
                "/home/repo/dest",
                "-e",
                "dev6",
                "-envs",
                "dev7, dev8",
                "-s",
                "s1,s2",
                "-q",
            )
        assertEquals("/home/repo", params.value("r"))
        assertEquals("/home/repo/dest", params.value("d"))
        assertEquals("dev6", params.value("e"))
        assertEquals(listOf("dev7", "dev8"), params.listValue("envs"))
        assertEquals(listOf("s1", "s2"), params.listValue("s"))
        assertEquals(emptyList<Any>(), params.listValue("empty"))
    }
}
