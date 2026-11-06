package io.microconfig.utils

import io.microconfig.utils.ConsoleColor.green
import io.microconfig.utils.ConsoleColor.red
import io.microconfig.utils.ConsoleColor.yellow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConsoleColorTest {
    @Test
    fun test() {
        if ((Os.isWindows())) {
            assertEquals("hello", green("hello"))
        } else {
            assertEquals("\u001b[32mhello\u001b[0m", green("hello"))
            assertEquals("\u001b[31mhello\u001b[0m", red("hello"))
            assertEquals("\u001b[33mhello\u001b[0m", yellow("hello"))
        }
    }
}
