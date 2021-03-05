package io.microconfig;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineParamParserTest {
    @Test
    void parse() {
        CommandLineParamParser params = CommandLineParamParser.parse("-r", "/home/repo", "-d", "/home/repo/dest", "-e", "dev6, dev7", "-s", "s1,s2", "-q");
        assertEquals("/home/repo", params.value("r"));
        assertEquals("/home/repo/dest", params.value("d"));
        assertEquals(asList("dev6", "dev7"), params.listValue("e"));
        assertEquals(asList("s1", "s2"), params.listValue("s"));
        assertEquals(emptyList(), params.listValue("empty"));
    }
}