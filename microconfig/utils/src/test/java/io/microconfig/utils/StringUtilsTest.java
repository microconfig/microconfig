package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.microconfig.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {
    @Test
    void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty(null));
        assertFalse(StringUtils.isEmpty("foo"));
    }

    @Test
    void testUnixLikePath() {
        assertEquals("B/ar", unixLikePath("B\\ar"));
    }

    @Test
    void testSplitToList() {
        List<String> list = new ArrayList<>();
        list.add("f");
        list.add("o");
        list.add("o");
        assertEquals(list, split("f,o,o", ","));
    }

    @Test
    void testAddOffsets() {
        assertEquals("foo", addOffsets("foo", 0));
        assertEquals("foo   ", addOffsets("foo", 3));
    }
}
