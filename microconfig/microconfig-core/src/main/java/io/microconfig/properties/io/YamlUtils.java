package io.microconfig.properties.io;

import io.microconfig.utils.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static io.microconfig.utils.FileUtils.LINE_SEPARATOR;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

public class YamlUtils {
    public static Map<String, String> readAsFlatMap(File file) {
        return new YamlReader().asFlatMap(file);
    }

    public static void write(File file, Map<String, String> properties, OpenOption... options) {
        new YamlWriter().write(file, properties, options);
    }

    private static class YamlReader {
        @SuppressWarnings("unchecked")
        public Map<String, String> asFlatMap(File file) {
            try (FileReader fileReader = new FileReader(file)) {
                Map<String, Object> map = new Yaml().loadAs(fileReader, Map.class);
                Map<String, Object> flatten = flat(map);
                return flatten.entrySet()
                        .stream()
                        .collect(toMap(Entry::getKey, e -> e.getValue().toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Map<String, Object> flat(Map<String, Object> source) {
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
    }

    private static class YamlWriter {
        public void write(File file, Map<String, String> properties, OpenOption... openOptions) {
            Map<String, Object> tree = toTree(properties);
            String withBlankLine = toYaml(tree);
            FileUtils.write(file.toPath(), withBlankLine, openOptions);
        }

        private Map<String, Object> toTree(Map<String, String> properties) {
            Map<String, Object> result = new TreeMap<>();
            properties.forEach((k, v) -> propertyToTree(k, v, result));
            return result;
        }

        @SuppressWarnings("unchecked")
        private void propertyToTree(String key, String value, Map<String, Object> result) {
            String[] parts = key.split("\\.");
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                Object oldValue = result.get(part);
                if (oldValue == null) {
                    Map<String, Object> newMap = new TreeMap<>();
                    result.put(part, newMap);
                    result = newMap;
                } else if (oldValue instanceof Map) {
                    result = (Map<String, Object>) oldValue;
                } else {
                    parts[i + 1] = part + "." + parts[i + 1];
                }
            }

            result.put(parts[parts.length - 1], value);
        }

        private String toYaml(Map<String, Object> tree) {
            StringBuilder result = new StringBuilder();
            return dump(result, tree, 0, true);
        }

        private String dump(StringBuilder result, Map<String, Object> tree, int indent, boolean emptyLine) {
            for (Entry<String, Object> entry : tree.entrySet()) {
                for (int i = 0; i < indent; i++) {
                    result.append(' ');
                }
                result.append(entry.getKey());
                dumpValue(result, entry.getValue(), indent + 2);
                if (emptyLine) {
                    result.append(LINE_SEPARATOR);
                }

            }
            return result.toString();
        }

        private void dumpValue(StringBuilder result, Object value, int indent) {
            if (value instanceof Map) {
                result.append(':').append(LINE_SEPARATOR);
                dump(result, (Map) value, indent, false);
            } else {
                result.append(": ").append(value).append(LINE_SEPARATOR);
            }
        }
    }
}