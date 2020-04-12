package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import static io.microconfig.utils.ConsoleColor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleColorTest {
    @Test
    void test() {
        assertEquals("\u001B[32mhello\u001B[0m", green("hello"));
        assertEquals("\u001B[31mhello\u001B[0m", red("hello"));
        assertEquals("\u001B[33mhello\u001B[0m", yellow("hello"));
    }
}