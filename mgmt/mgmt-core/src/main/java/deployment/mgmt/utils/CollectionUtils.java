package deployment.mgmt.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class CollectionUtils {
    public static <T> Set<T> findDuplicates(Collection<T> collection) {
        return collection.stream()
                .collect(groupingBy(identity(), counting()))
                .entrySet()
                .stream().filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());
    }
}
