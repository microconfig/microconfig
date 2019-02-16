package io.microconfig.utils;

import java.util.Collection;

public class CollectionUtils {
    public static <T> T singleValue(Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Incorrect collection size " + values.size());
        }

        return values.iterator().next();
    }
}