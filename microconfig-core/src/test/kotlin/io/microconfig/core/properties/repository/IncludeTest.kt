package io.microconfig.core.properties.repository

import io.microconfig.core.properties.repository.Include.Companion.parse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class IncludeTest {
    @Test
    fun testParse() {
        testInclude("comp", "env", parse("comp[env]", "dev"))
        testInclude("c.d_-q2", "dev", parse("c.d_-q2", "dev"))
        assertThrows(IllegalArgumentException::class.java, { parse("*", "e") })
    }

    private fun testInclude(expectedComp: String, expectedEnv: String, actual: Include) {
        assertEquals(expectedComp, actual.component)
        assertEquals(expectedEnv, actual.environment)
    }

    @Test
    fun testToString() {
        assertEquals("#include comp[dev]", parse("comp", "dev").toString())
    }

    @Test
    fun trimAndOmitEmptyNamesInIncludeLists() {
        val includes = Includes.from("#include first, ,\tsecond[test]\t, ").withDefaultEnv("dev")
        assertEquals(listOf(Include("first", "dev"), Include("second", "test")), includes)
    }

    @Test
    fun rejectEmptyIncludeLists() {
        assertThrows(IllegalArgumentException::class.java) {
            Includes.from("#include , \t, ").withDefaultEnv("dev")
        }
    }
}
