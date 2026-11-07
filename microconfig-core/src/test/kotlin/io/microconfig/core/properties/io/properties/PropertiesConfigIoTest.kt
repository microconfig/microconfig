package io.microconfig.core.properties.io.properties

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.properties.io.properties.impl.PropertiesConfigIo
import io.microconfig.io.impl.DumpedFsReader
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class PropertiesConfigIoTest {
    val ioService: PropertiesConfigIo = PropertiesConfigIo(DumpedFsReader())

    @Test
    fun test() {
        val file: File = classpathFile("configFormats/properties/propLine.properties")
        assertEquals(expectedMap(), ioService.readFrom(file).propertiesAsMap())
    }

    private fun expectedMap(): Map<String, String> {
        val multilineEscape: String = "\\" + LINES_SEPARATOR
        val expected = TreeMap<String, String>()
        expected.put("p", "p_v")
        expected.put("p2", "p2_v")
        expected.put("p3", "=p3_v")
        expected.put("p4", ":p4_v")
        expected.put(
            "jwt.publicKey",
            "-----BEGIN PUBLIC KEY-----" +
                    multilineEscape +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgfjVb7pJlEdu9lDPOxmi" +
                    multilineEscape +
                    "LwIDAQAB" +
                    multilineEscape +
                    "-----END PUBLIC KEY-----",
        )
        expected.put("empty", "")
        expected.put("empty2", "")
        return expected
    }
}
