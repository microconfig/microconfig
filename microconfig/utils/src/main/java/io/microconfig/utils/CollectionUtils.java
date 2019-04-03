package io.microconfig.utils;

import java.util.*;

import static java.util.Arrays.asList;

public class CollectionUtils {
    public static <T> T singleValue(Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Incorrect collection size " + values.size());
        }

        return values.iterator().next();
    }

    public static <T> List<T> join(Collection<T> first, Collection<T> second) {
        List<T> list = new ArrayList<>();
        list.addAll(first);
        list.addAll(second);
        return list;
    }

    public static <T> Set<T> joinToSet(Collection<T> first, Collection<T> second) {
        Set<T> list = new HashSet<>();
        list.addAll(first);
        list.addAll(second);
        return list;
    }

    public static <T> Set<T> setOf(T... t) {
        return new LinkedHashSet<>(asList(t));
    }
}