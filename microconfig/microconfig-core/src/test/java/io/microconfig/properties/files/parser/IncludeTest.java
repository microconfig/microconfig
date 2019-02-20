package io.microconfig.properties.files.parser;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class IncludeTest {
    @Test
    void testMatch() {
        Matcher matcher = Include.PATTERN.matcher("#include zeus[uat]");

        assertTrue(matcher.find());
        assertEquals("zeus", matcher.group("comp"));
        assertEquals("uat", matcher.group("env"));

        matcher = Include.PATTERN.matcher("#include zeus");

        assertTrue(matcher.find());
        assertEquals("zeus", matcher.group("comp"));
        assertNull(matcher.group("env"));
    }

    @Test
    void testMatchWithout() {
        Matcher matcher = Include.PATTERN.matcher("#include zeus[uat] @without:ru.sbt.*");

        assertTrue(matcher.find());
        assertEquals("zeus", matcher.group("comp"));
        assertEquals("uat", matcher.group("env"));
        assertEquals("ru.sbt.*", matcher.group("without"));
    }

    @Test
    void testDontMatch() {
        Matcher matcher = Include.PATTERN.matcher("include zeus[uat]");
        assertFalse(matcher.find());

        matcher = Include.PATTERN.matcher("#iclude zeus");
        assertFalse(matcher.find());
    }
}
