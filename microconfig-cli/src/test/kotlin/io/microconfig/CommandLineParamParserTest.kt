package io.microconfig

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class CommandLineParamParserTest {
    @ParameterizedTest
    @MethodSource("versionArguments")
    fun parseVersionFlagWithoutConsumingTheFollowingOption(arguments: Array<String>) {
        val params = CommandLineParamParser.parse(*arguments)

        assertTrue(params.contains("v"))
        assertNull(params.value("v"))
        assertEquals("false", params.value("color"))
        assertEquals("repo", params.value("r"))
    }

    @Test
    fun keepFlagLikeValuesOfOtherOptions() {
        val params = CommandLineParamParser.parse("-r", "-v", "-d", "-output", "-e", "dev")

        assertEquals("-v", params.value("r"))
        assertEquals("-output", params.value("d"))
        assertEquals("dev", params.value("e"))
        assertTrue(!params.contains("v"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["unexpected", ""])
    fun reportMalformedKeysToTheCaller(key: String) {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CommandLineParamParser.parse("-color", "false", key)
        }
        assertEquals("key '$key' must start with -", error.message)
    }

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

    companion object {
        @JvmStatic
        fun versionArguments(): List<Arguments> = listOf(
            arrayOf("-v", "-color", "false", "-r", "repo"),
            arrayOf("-color", "false", "-v", "-r", "repo"),
            arrayOf("-color", "false", "-r", "repo", "-v"),
            arrayOf("-v", "-v", "-color", "false", "-r", "repo", "-v"),
        ).map { Arguments.of(it) }
    }
}
