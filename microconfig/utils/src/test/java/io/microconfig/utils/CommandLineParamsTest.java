package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineParamsTest {
    @Test
    void parse() {
        CommandLineParams params = CommandLineParams.parse("-r", "/home/repo", "-d", "/home/repo/dest", "-e", "dev6", "-s", "s1,s2", "-q");
        assertEquals("/home/repo", params.value("r"));
        assertEquals("/home/repo/dest", params.value("d"));
        assertEquals("dev6", params.value("e"));
        assertEquals(asList("s1", "s2"), params.listValue("s"));
        assertEquals(emptyList(), params.listValue("empty"));
    }
}