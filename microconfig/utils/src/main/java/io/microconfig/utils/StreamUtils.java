package io.microconfig.utils;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class StreamUtils {
    public static <K, V> List<V> forEach(Collection<K> collection,
                                         Function<? super K, ? extends V> applyFunction) {
        return forEach(collection.stream(), applyFunction);
    }

    public static <K, V> List<V> forEach(Stream<K> stream,
                                         Function<? super K, ? extends V> applyFunction) {
        return stream.map(applyFunction).collect(toList());
    }

    public static <K, V, T> T forEach(Collection<K> collection,
                                      Function<? super K, ? extends V> applyFunction,
                                      Collector<? super V, ?, T> collector) {
        return collection.stream().map(applyFunction).collect(collector);
    }

    public static <K, V> List<V> flatMapEach(Collection<K> collection,
                                             Function<? super K, ? extends Collection<? extends V>> function) {
        return collection.stream()
                .flatMap(k -> function.apply(k).stream())
                .collect(toList());
    }

    public static <T> List<T> filter(Collection<T> collection,
                                     Predicate<? super T> predicate) {
        return collection.stream()
                .filter(predicate)
                .collect(toList());
    }

    public static <K, V> Optional<V> findFirstResult(Collection<K> collection, Function<K, Optional<V>> getter) {
        return collection.stream()
                .map(getter)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
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