package io.microconfig.properties.io;

import io.microconfig.properties.Property;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.properties.Property.typeValue;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

public class YamlUtils {
    public static Map<String, String> asFlatMap(File file) {
        try (FileReader fileReader = new FileReader(file)) {
            Map<String, Object> map = new Yaml().loadAs(fileReader, Map.class);
            Map<String, Object> flatten = flat(map);
            return flatten.entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> flat(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (String key : source.keySet()) {
            Object value = source.get(key);

            if (value instanceof Map) {
                Map<String, Object> subMap = flat((Map<String, Object>) value);

                for (String subkey : subMap.keySet()) {
                    result.put(key + "." + subkey, subMap.get(subkey));
                }
            } else if (value instanceof Collection) {
                StringBuilder joiner = new StringBuilder();
                String separator = "";

                for (Object element : ((Collection) value)) {
                    Map<String, Object> subMap = flat(singletonMap(key, element));
                    joiner.append(separator)
                            .append(subMap.entrySet().iterator().next().getValue().toString());

                    separator = ",";
                }

                result.put(key, joiner.toString());
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    public static Map<String, Object> toTree(Collection<Property> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.stream()
                .filter(p -> !p.isTemp())
                .forEach(p -> add(p.getKey(), p.typedValue(), result));
        return result;
    }

    public static Map<String, Object> toTree(Map<String, String> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.forEach((k, v) -> add(k, typeValue(v), result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void add(String key, Object value, Map<String, Object> result) {
        String[] split = key.split("\\.");
        for (int i = 0; i < split.length - 1; i++) {
            String part = split[i];
            result = (Map<String, Object>) result.compute(part, (k, v) -> {
                if (v == null) return new TreeMap<>();
                if (v instanceof Map) return v;
                TreeMap<Object, Object> map = new TreeMap<>();
                map.put(k, v);
                return map;
            });
        }

        result.put(split[split.length - 1], value);
    }
}
