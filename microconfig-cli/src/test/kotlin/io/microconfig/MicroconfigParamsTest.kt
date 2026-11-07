package io.microconfig

import io.microconfig.MicroconfigParams.Companion.parse
import java.io.File
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MicroconfigParamsTest {
    private val params =
        parse(
            "-r",
            "configs/repo",
            "-d",
            "destination",
            "-e",
            "dev",
            "-envs",
            "dev, test",
            "-g",
            "g1,g2",
            "-s",
            "s1, s2",
        )
    private val empty = parse()
    private val singleEnvBuild = parse("-e", "dev")

    @Test
    fun rootDir() {
        assertEquals(File("configs/repo"), params.rootDir())
    }

    @Test
    fun destinationDir() {
        assertEquals("destination", params.destinationDir())
        assertEquals("build", empty.destinationDir())
    }

    @Test
    fun env() {
        assertEquals("dev", params.environments().first())
    }

    @Test
    fun envs() {
        assertEquals(linkedSetOf("dev", "test"), params.environments())
    }

    @Test
    fun groups() {
        assertEquals(listOf("g1", "g2"), params.groups())
        assertEquals(emptyList<String>(), empty.groups())
    }

    @Test
    fun services() {
        assertEquals(listOf("s1", "s2"), params.services())
        assertEquals(emptyList<String>(), empty.services())
    }

    @Test
    fun color() {
        assertTrue(empty.color())
        assertTrue(parse("-color", "true").color())
        assertFalse(parse("-color", "false").color())
    }

    @Test
    fun testVersion() {
        assertTrue(parse("-v").version())
    }

    @Test
    fun isSingleEnvBuild() {
        assertFalse(params.isSingleEnvBuild())
        assertTrue(singleEnvBuild.isSingleEnvBuild())
    }
}
