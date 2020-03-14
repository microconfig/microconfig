package io.microconfig.io;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class StreamUtils {
    public static <K, V> List<V> toList(Collection<K> collection, Function<? super K, ? extends V> function) {
        return collection.stream()
                .map(function)
                .collect(Collectors.toList());
    }

    public static <K, V> List<V> flatMap(Collection<K> collection,
                                         Function<? super K, ? extends Collection<? extends V>> function) {
        return collection.stream()
                .flatMap(k -> function.apply(k).stream())
                .collect(Collectors.toList());
    }

    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        return collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
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