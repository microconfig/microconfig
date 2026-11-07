package io.microconfig

import io.microconfig.utils.Logger
import io.microconfig.utils.IoUtils.readClasspathResource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@ResourceLock(Resources.SYSTEM_OUT)
class ConsoleColorOptionTest {
    @AfterEach
    fun restoreColorOutput() {
        Logger.enableColors(true)
    }

    @Test
    fun disableColorsAndRestoreDefaultOnNextInvocation() {
        assertEquals("warning\nsuccess\nfailure\n", logMessages("-color", "false", "-v"))
        assertEquals(
            "\u001B[33mwarning\u001B[0m\n" +
                "\u001B[32msuccess\u001B[0m\n" +
                "\u001B[31mfailure\u001B[0m\n",
            logMessages("-v"),
        )
    }

    @ParameterizedTest(name = "{index}: colors={0}")
    @MethodSource("malformedArguments")
    fun applyColorOptionBeforeReportingMalformedArguments(colors: Boolean, arguments: Array<String>) {
        val process = startCli(arguments)
        try {
            assertTrue(process.waitFor(10, SECONDS), "CLI did not exit after invalid arguments")
            val output = process.inputStream.bufferedReader(UTF_8).use { it.readText() }
            assertTrue(process.exitValue() == 255 || process.exitValue() == -1, output)
            assertTrue(output.contains("must start with -"), output)
            assertEquals(colors, output.contains("\u001B["), output)
            assertTrue(!output.contains("Exception in thread"), output)
        } finally {
            process.destroyForcibly()
        }
    }

    @ParameterizedTest
    @MethodSource("versionArguments")
    fun printVersionRegardlessOfFlagPosition(arguments: Array<String>) {
        val process = startCli(arguments)
        try {
            assertTrue(process.waitFor(10, SECONDS), "CLI did not print its version")
            val output = process.inputStream.bufferedReader(UTF_8).use { it.readText() }
            assertEquals(0, process.exitValue(), output)
            assertEquals(readClasspathResource("version.properties").substringAfter("version=").trim(), output.trim())
        } finally {
            process.destroyForcibly()
        }
    }

    private fun startCli(arguments: Array<String>): Process =
        ProcessBuilder(
            listOf(File(System.getProperty("java.home"), "bin/java").path,
                "-cp", runtimeClasspath(), "io.microconfig.MicroconfigMainKt") + arguments
        ).redirectErrorStream(true).start()

    private fun runtimeClasspath(): String {
        val loaderPaths = generateSequence(javaClass.classLoader) { it.parent }
            .filterIsInstance<URLClassLoader>()
            .flatMap { it.urLs.asSequence() }
            .map { File(it.toURI()).path }
        return (System.getProperty("java.class.path").split(File.pathSeparator).asSequence() + loaderPaths)
            .distinct().joinToString(File.pathSeparator)
    }

    private fun logMessages(vararg args: String): String {
        val output = ByteArrayOutputStream()
        val previous = System.out
        PrintStream(output, true, UTF_8).use { stream ->
            try {
                System.setOut(stream)
                MicroconfigMain.main(*args)
                output.reset()
                Logger.warn("warning")
                Logger.announce("success")
                Logger.error("failure")
            } finally {
                System.setOut(previous)
            }
        }
        return output.toString(UTF_8).replace("\r\n", "\n")
    }

    companion object {
        @JvmStatic
        fun versionArguments(): List<Arguments> = listOf(
            arrayOf("-v", "-color", "false"),
            arrayOf("-color", "false", "-v"),
            arrayOf("-v", "-r", "missing", "-color", "false"),
            arrayOf("-v", "-v", "-color", "false", "-v"),
        ).map { Arguments.of(it) }

        @JvmStatic
        fun malformedArguments(): List<Arguments> = listOf(
            Arguments.of(false, arrayOf("-color", "false", "unexpected")),
            Arguments.of(false, arrayOf("unexpected", "-color", "false")),
            Arguments.of(false, arrayOf("", "-color", "false")),
            Arguments.of(false, arrayOf("-color", "\"false\"", "unexpected")),
            Arguments.of(false, arrayOf("-color", "true", "unexpected", "-color", "false")),
            Arguments.of(true, arrayOf("-color", "false", "unexpected", "-color", "true")),
            Arguments.of(true, arrayOf("-color", "false", "unexpected", "-color", "\"true\"")),
            Arguments.of(false, arrayOf("unexpected", "-color", "invalid")),
            Arguments.of(true, arrayOf("unexpected")),
            Arguments.of(false, arrayOf("-unknown", "ignored", "unexpected", "-color", "false")),
            Arguments.of(true, arrayOf("-unknown", "-color", "unexpected")),
        )
    }
}
