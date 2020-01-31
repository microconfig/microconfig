package io.microconfig.utils;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class StreamUtils {
    public static <T, R> List<R> map(Collection<T> collection, Function<? super T, ? extends R> function) {
        return collection.stream()
                .map(function)
                .collect(toList());
    }

    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        return collection.stream()
                .filter(predicate)
                .collect(toList());
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(Function<? super T, ? extends K> keyMapper,
                                                                   Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), LinkedHashMap::new);
    }

    public static <T, K, U> Collector<T, ?, SortedMap<K, U>> toSortedMap(Function<? super T, ? extends K> keyMapper,
                                                                         Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), TreeMap::new);
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(format("Duplicate key %s", u));
        };
    }
}