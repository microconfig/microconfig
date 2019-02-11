package deployment.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class CollectionUtils {
    public static <K, V> Map<K, V> join(Map<? extends K, ? extends V> first, Map<? extends K, ? extends V> second, Map<K, V> destination) {
        destination.putAll(first);
        destination.putAll(second);

        return destination;
    }

    public static <K, V> Map<K, V> join(Map<? extends K, ? extends V> first, Map<? extends K, ? extends V> second) {
        return join(first, second, new LinkedHashMap<>());

    }

    public static <T> T singleValue(Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Incorrect collection size " + values.size());
        }

        return values.iterator().next();
    }

    public static <T> Set<T> findDuplicates(Collection<T> collection) {
        return collection.stream()
                .collect(groupingBy(identity(), counting()))
                .entrySet()
                .stream().filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());
    }
}