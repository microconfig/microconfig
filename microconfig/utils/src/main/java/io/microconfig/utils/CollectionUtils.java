package io.microconfig.utils;

import java.util.*;
import java.util.stream.Stream;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.lang.Math.max;
import static java.util.Arrays.asList;

public class CollectionUtils {
    public static <T> T singleValue(Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Expected single element, actual: " + values.size());
        }

        return values.iterator().next();
    }

    public static <T> List<T> join(Collection<T> first, Collection<T> second) {
        List<T> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    public static <T> List<T> minus(Collection<T> first, Collection<T> second) {
        ArrayList<T> result = new ArrayList<>(first);
        result.removeAll(second);
        result.trimToSize();
        return result;
    }

    public static <T> Set<T> joinToSet(Collection<T> first, Collection<T> second) {
        Set<T> list = new HashSet<>(max(first.size(), second.size()));
        list.addAll(first);
        list.addAll(second);
        return list;
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... t) {
        return new LinkedHashSet<>(asList(t));
    }

    public static Map<String, String> splitKeyValue(String... keyValue) {
        return Stream.of(keyValue)
                .map(s -> s.split("="))
                .collect(toLinkedMap(s -> s[0], s -> s.length == 1 ? "" : s[1]));
    }
}