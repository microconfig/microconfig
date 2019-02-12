package io.microconfig.utils;

import org.junit.Test;

import static io.microconfig.utils.StringUtils.like;
import static io.microconfig.utils.StringUtils.toLowerHyphen;
import static org.junit.Assert.*;

public class StringUtilsTest {
    @Test
    public void testLike() {
        assertFalse(like(null, null));
        assertFalse(like("", null));
        assertFalse(like(null, ""));
        assertTrue(like("", ""));
        assertTrue(like("", "%"));
        assertTrue(like("a", "%"));
        assertTrue(like("ab", "%"));
        assertTrue(like("abc", "%"));
        assertTrue(like("abc", "a%"));
        assertTrue(like("abc", "ab%"));
        assertTrue(like("abc", "abc%"));
        assertTrue(like("abc", "%c"));
        assertTrue(like("abc", "%bc"));
        assertTrue(like("abc", "%abc"));
        assertTrue(like("abc", "%%"));
        assertTrue(like("abc", "%a%"));
        assertTrue(like("abc", "%ab%"));
        assertTrue(like("abc", "%abc%"));
        assertTrue(like("abc", "a%bc%"));
        assertTrue(like("abc", "a%b%c"));
        assertTrue(like("abc", "a%%c"));
        assertTrue(like("abc", "ab%%c"));
        assertTrue(like("abc", "%a%b%c%"));
        assertFalse(like("abc", "%a%bb%c%"));
        assertFalse(like("abc", "%aa%"));
        assertFalse(like("abc", "%a%a%"));
        assertFalse(like("abc", "a%a%"));
        assertFalse(like("abc", "a%cc"));
        assertFalse(like("abc", "abcc"));
        assertFalse(like("abc", "ABC"));
        assertFalse(like("", "_"));
        assertTrue(like("a", "_"));
        assertTrue(like("ab", "__"));
        assertTrue(like("abc", "___"));
        assertTrue(like("abc", "a__"));
        assertTrue(like("abc", "a_c"));
        assertTrue(like("abc", "_b_"));
        assertFalse(like("abc", "__"));
        assertFalse(like("abc", "_a_"));
        assertFalse(like("abc", "_bb_"));
        assertFalse(like("abc", "_abc"));
        assertFalse(like("abc", "abc_"));
        assertTrue(like("abc", "%_"));
        assertTrue(like("abc", "_%"));
        assertTrue(like("abc", "_%_"));
        assertTrue(like("abc", "_%_%_"));
        assertTrue(like("abc", "_%bc"));
        assertTrue(like("abc", "ab_%"));
        assertTrue(like("abc", "ab%_"));
        assertTrue(like("abc", "a%_"));
        assertFalse(like("abc", "abc%_"));
        assertFalse(like("abc", "_%bc%_"));
        assertTrue(like("abc", "_%b%_"));
        assertTrue(like("ru.sbt.risk.hmds.local.jdbc.password", "ru.sbt.risk.hmds.local%"));
    }

    @Test
    public void testToLowerHyphen() {
        assertEquals("hello-world", toLowerHyphen("helloWorld"));
        assertEquals("yes", toLowerHyphen("yes"));
    }
}