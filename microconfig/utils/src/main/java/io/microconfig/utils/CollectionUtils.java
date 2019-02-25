package io.microconfig.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {
    public static <T> T singleValue(Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Incorrect collection size " + values.size());
        }

        return values.iterator().next();
    }

    public static<T> List<T> join(List<T> first, List<T> second) {
        List<T> list = new ArrayList<>();
        list.addAll(first);
        list.addAll(second);
        return list;
    }
}