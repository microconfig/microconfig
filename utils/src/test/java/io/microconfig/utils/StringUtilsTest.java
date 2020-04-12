package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.StringUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {
    @Test
    void testIsEmpty() {
        assertTrue(isEmpty(""));
        assertTrue(isEmpty(null));
        assertFalse(isEmpty(" "));
        assertFalse(isEmpty("foo"));
    }

    @Test
    void testSplit() {
        List<String> expected = asList("f", "o.o", "o");
        assertEquals(expected, split("  f  ,  o.o , o", ","));
        assertEquals(emptyList(), split("  ", ","));
        assertEquals(emptyList(), split("", ","));
    }

    @Test
    void testSplitKeyValue() {
        Map<String, String> expected = new HashMap<>();
        expected.put("k1", "v1");
        expected.put("k2", "v2");
        expected.put("k3", "");
        assertEquals(
                expected,
                splitKeyValue("k1=v1", "k2=v2", "k3=")
        );
    }

    @Test
    void testFindFirstIndexIn() {
        String line = "1234";
        assertEquals(1, findFirstIndexIn(line, "432"));
        assertEquals(3, findFirstIndexIn(line, "747"));
        assertEquals(-1, findFirstIndexIn(line, "f"));
    }

    @Test
    void testAddOffsets() {
        assertEquals("foo", addOffsets("foo", 0));
        assertEquals("foo   ", addOffsets("foo", 3));
    }

    @Test
    void testSymbolCount() {
        String line = "helloworld";
        assertEquals(3, symbolCountIn(line, 'l'));
        assertEquals(2, symbolCountIn(line, 'o'));
        assertEquals(0, symbolCountIn(line, 'z'));
        assertEquals(0, symbolCountIn("", 'z'));
        assertEquals(4, dotCountIn("1fdw.234.4.."));
        assertEquals(0, dotCountIn(line));
    }

    @Test
    void testUnixLikePath() {
        assertEquals("B/ar", unixLikePath("B\\ar"));
    }

    @Test
    void testEscape() {
        assertEquals("\\\\regex\\\\w+", escape("\\regex\\w+"));
    }

    @Test
    void testAsStringBuilder() {
        StringBuilder expected = new StringBuilder("line");
        StringBuilder actual = asStringBuilder("line");
        assertEquals(StringBuilder.class, actual.getClass());
        assertEquals(expected.toString(), actual.toString());
        assertSame(expected, asStringBuilder(expected));
    }
}
