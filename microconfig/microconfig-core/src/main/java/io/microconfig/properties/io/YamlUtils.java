package io.microconfig.properties.io;

import io.microconfig.utils.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

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
import static org.yaml.snakeyaml.nodes.Tag.*;

public class YamlUtils {
    public static Map<String, String> readAsFlatMap(File file) {
        return new YamlReader().asFlatMap(file);
    }

    public static void write(File file, Map<String, String> properties, OpenOption... options) {
        new YamlWriter().write(file, properties, options);
    }

    private static class YamlReader {
        public Map<String, String> asFlatMap(File file) {
            try (FileReader fileReader = new FileReader(file)) {
                Map<String, Object> map = new Yaml(new ToStringConstructor()).load(fileReader);
                Map<String, Object> flatten = flat(map);
                return flatten.entrySet()
                        .stream()
                        .collect(toMap(Entry::getKey, e -> e.getValue() == null ? "" : e.getValue().toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> flat(Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Object key : source.keySet()) {
                Object value = source.get(key);

                if (value instanceof Map) {
                    Map<?, ?> subMap = flat((Map<String, Object>) value);

                    for (Object subkey : subMap.keySet()) {
                        result.put(key + "." + subkey, subMap.get(subkey));
                    }
                } else if (value instanceof Collection) {
                    StringBuilder joiner = new StringBuilder();
                    String separator = "";

                    for (Object element : ((Collection) value)) {
                        Map<?, ?> subMap = flat(singletonMap(key, element));
                        joiner.append(separator)
                                .append(subMap.entrySet().iterator().next().getValue().toString());

                        separator = ",";
                    }

                    result.put(key.toString(), joiner.toString());
                } else {
                    result.put(key.toString(), value);
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

        @SuppressWarnings("unchecked")
        private void dumpValue(StringBuilder result, Object value, int indent) {
            if (value instanceof Map) {
                result.append(':').append(LINE_SEPARATOR);
                dump(result, (Map) value, indent, false);
            } else {
                result.append(": ").append(value).append(LINE_SEPARATOR);
            }
        }
    }

    private static class ToStringConstructor extends Constructor {
        ToStringConstructor() {
            this.yamlConstructors.put(INT, ToStringConstruct.INSTANCE);
            this.yamlConstructors.put(FLOAT, ToStringConstruct.INSTANCE);
            this.yamlConstructors.put(TIMESTAMP, ToStringConstruct.INSTANCE);
        }

        private static class ToStringConstruct extends AbstractConstruct {
            private static final ToStringConstruct INSTANCE = new ToStringConstruct();

            public Object construct(Node node) {
                return ((ScalarNode) node).getValue();
            }
        }
    }
}