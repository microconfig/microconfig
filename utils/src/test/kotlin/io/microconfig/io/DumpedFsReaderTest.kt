package io.microconfig.io

import io.microconfig.io.impl.DumpedFsReader
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import io.microconfig.utils.IoUtilsTest.Companion.resourceFile
import java.util.Optional.empty
import java.util.Optional.of
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DumpedFsReaderTest {
    val reader: FsReader = DumpedFsReader()

    @Test
    fun readFully() {
        val expected: String = "k1:" + LINES_SEPARATOR + "  k2: v2"
        assertEquals(expected, reader.readFully(resourceFile()))
    }

    @Test
    fun readLines() {
        assertEquals(listOf("k1:", "  k2: v2"), reader.readLines(resourceFile()))
    }

    @Test
    fun firstLineOf() {
        assertEquals(of("  k2: v2"), reader.firstLineOf(resourceFile(), { s -> s.contains("k2") }))
        assertEquals(empty<Any>(), reader.firstLineOf(resourceFile(), { s -> s.contains("eeee") }))
    }
}
