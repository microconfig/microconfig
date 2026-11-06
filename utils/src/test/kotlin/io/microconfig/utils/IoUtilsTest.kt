package io.microconfig.utils

import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import io.microconfig.utils.IoUtils.readFully
import io.microconfig.utils.IoUtils.readLines
import java.io.File
import java.io.FileInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IoUtilsTest {
    @Test
    fun testReadFully() {
        val expected: String = "k1:" + LINES_SEPARATOR + "  k2: v2"
        assertEquals(expected, readFully(resourceFile()))
        assertEquals(expected, readFully(FileInputStream(resourceFile())))
    }

    @Test
    fun testReadLines() {
        assertEquals(listOf("k1:", "  k2: v2"), readLines(resourceFile()))
    }

    companion object {
        fun resourceFile(): File {
            return File(IoUtils::class.java.classLoader.getResource("file.yaml").file)
        }
    }
}
