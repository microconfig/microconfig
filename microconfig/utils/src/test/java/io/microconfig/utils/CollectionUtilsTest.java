package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import static io.microconfig.utils.CollectionUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionUtilsTest {
    @Test
    void testSingleValue() {
        assertEquals("1", singleValue(singletonList("1")));
        assertThrows(IllegalArgumentException.class, () -> singleValue(asList("1", "2")));
        assertThrows(IllegalArgumentException.class, () -> singleValue(emptyList()));
    }

    @Test
    void testJoin() {
        {
            List<Integer> expected = asList(1, 3, 2, 4);
            List<Integer> l1 = asList(1, 3);
            List<Integer> l2 = asList(2, 4);
            assertEquals(expected, join(l1, l2));
            assertEquals(new HashSet<>(expected), joinToSet(l1, l2));
        }

        {
            List<Integer> expected = asList(1, 4);
            List<Integer> l1 = emptyList();
            List<Integer> l2 = asList(1, 4);
            assertEquals(expected, join(l1, l2));
            assertEquals(new HashSet<>(expected), joinToSet(l1, l2));
        }
    }

    @Test
    void testMinus() {
        assertEquals(
                asList(2, 4, 4, 5),
                minus(asList(1, 1, 2, 3, 4, 4, 5), asList(1, 3, 3, 56))
        );
    }

    @Test
    void testSetOf() {
        assertEquals(new LinkedHashSet<>(asList(3, 1, 2)), setOf(3, 1, 2, 3, 1, 2));
    }
}