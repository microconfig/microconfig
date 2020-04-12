package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static io.microconfig.utils.StreamUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamUtilsTest {
    List<Integer> elements = asList(2, 6, 3);

    @Test
    void testForEach() {
        List<String> expected = asList("2", "6", "3");
        assertEquals(expected, forEach(elements, String::valueOf));
        assertEquals(expected, forEach(elements.stream(), String::valueOf));
        assertEquals(expected, forEach(elements, String::valueOf, toCollection(ArrayList::new)));
    }

    @Test
    void testFlatMapEach() {
        assertEquals(
                asList("2", "22", "6", "66", "3", "33"),
                flatMapEach(elements, e -> asList("" + e, e + "" + e))
        );
    }

    @Test
    void testFilter() {
        assertEquals(asList(6, 3), filter(elements, e -> e > 2));
        assertEquals(asList(6, 3), filter(elements, e -> e > 2, toCollection(ArrayList::new)));
    }

    @Test
    void testFindFirstResult() {
        assertEquals(of(6), findFirstResult(elements, e -> e > 2 ? of(e) : empty()));
        assertEquals(empty(), findFirstResult(elements, e -> empty()));
    }

    @Test
    void testToMap() {
        Map<Integer, String> lhm = Stream.of(1).collect(toLinkedMap(identity(), String::valueOf));
        Map<Integer, String> tm = Stream.of(1).collect(toSortedMap(identity(), String::valueOf));
        assertEquals(LinkedHashMap.class, lhm.getClass());
        assertEquals(TreeMap.class, tm.getClass());

        Map<Integer, String> expected = singletonMap(1, "1");
        assertEquals(expected, lhm);
        assertEquals(expected, tm);
    }

    @Test
    void testToMapNotUniqueKey() {
        List<Integer> elements = asList(1, 1);
        assertThrows(IllegalStateException.class, () -> elements.stream().collect(toLinkedMap(identity(), identity())));
        assertThrows(IllegalStateException.class, () -> elements.stream().collect(toSortedMap(identity(), identity())));
    }
}