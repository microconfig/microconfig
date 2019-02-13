package io.microconfig.utils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class CollectionUtils {
    public static <K, V> Map<K, V> join(Map<? extends K, ? extends V> first,
                                        Map<? extends K, ? extends V> second) {
        Map<K, V> destination = new LinkedHashMap<>();
        destination.putAll(first);
        destination.putAll(second);

        return destination;

    }

    public static <T> T singleValue(Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Incorrect collection size " + values.size());
        }

        return values.iterator().next();
    }
}