package io.microconfig.core.properties.io.yaml;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.addOffsets;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class YamlTreeImpl implements YamlTree {
    private static final int OFFSET = 2;
    private final boolean addEmptyLineAfterSections;

    public YamlTreeImpl() {
        this(true);
    }

    public YamlTreeImpl(boolean addEmptyLineAfterSections) {
        this.addEmptyLineAfterSections = addEmptyLineAfterSections;
    }

    @Override
    public String toYaml(Map<String, String> flatProperties) {
        Map<String, Object> tree = new TreeCreator().toTree(flatProperties);
        return new YamlDumper().toYamlFromTree(tree);
    }

    static class TreeCreator {
        private static final Gson STRING_ESCAPER = new Gson();

        Map<String, Object> toTree(Map<String, String> flatProperties) {
            if (!(flatProperties instanceof TreeMap) || ((TreeMap<?, ?>) flatProperties).comparator() != null) {
                flatProperties = new TreeMap<>(flatProperties);
            }

            Map<String, Object> result = new TreeMap<>();
            flatProperties.forEach((k, v) -> propertyToTree(k, v, result));
            return result;
        }

        @SuppressWarnings("unchecked")
        private void propertyToTree(String key, String value, Map<String, Object> result) {
            List<String> parts = splitKey(key);
            for (int i = 0; i < parts.size() - 1; i++) {
                String part = parts.get(i);
                Object oldValue = result.get(part);
                if (oldValue == null) {
                    Map<String, Object> newMap = new TreeMap<>();
                    result.put(part, newMap);
                    result = newMap;
                } else if (oldValue instanceof Map) {
                    result = (Map<String, Object>) oldValue;
                } else {
                    parts.set(i + 1, part + "." + parts.get(i + 1));
                }
            }

            result.put(parts.get(parts.size() - 1), offsetForMultilineValue(parts.size(), value));
        }

        private String offsetForMultilineValue(int parts, String value) {
            if (value.startsWith("-")) {
                return withOffsets(parts, value);
            }

            if (value.contains(LINES_SEPARATOR)) {
                return "|" + withOffsets(parts, value);
            }

            return quoteAmbiguousScalar(value);
        }

        private String quoteAmbiguousScalar(String value) {
            if (value.startsWith("[") || value.startsWith("{") || value.startsWith("\"") || value.startsWith("'")) return value;
            return value.contains(": ") || value.contains(" #") || value.startsWith("#") || value.endsWith(":")
                    ? STRING_ESCAPER.toJson(value) : value;
        }

        // empty lines of the value stay empty, without trailing spaces of the offset
        private String withOffsets(int parts, String value) {
            String offset = addOffsets("", parts * OFFSET);
            return Stream.of(value.split(quote(LINES_SEPARATOR), -1)) //todo test on win
                    .map(line -> line.isEmpty() ? line : offset + line)
                    .collect(joining(LINES_SEPARATOR, LINES_SEPARATOR, ""));
        }

        //split on '.' that are not inside of [] or ""
        private List<String> splitKey(String key) {
            if (key.isEmpty()) return emptyList();

            int insideBrackets = 0;
            boolean insideQuotes = false;
            int last = 0;

            List<String> results = new ArrayList<>();
            for (int i = 0; i < key.length(); i++) {
                if (key.charAt(i) == '[') ++insideBrackets;
                if (key.charAt(i) == ']') insideBrackets = max(0, insideBrackets - 1);
                if (key.charAt(i) == '"') insideQuotes = !insideQuotes;

                if (key.charAt(i) == '.' && insideBrackets == 0 && !insideQuotes) {
                    results.add(key.substring(last, i));
                    if (i + 1 >= key.length()) return results;

                    last = i + 1;
                }
            }

            results.add(key.substring(last));
            return results;
        }
    }

    class YamlDumper {
        private final StringBuilder result = new StringBuilder();

        String toYamlFromTree(Map<String, Object> tree) {
            dump(tree, 0, true);

            result.setLength(Math.max(0, result.length() - LINES_SEPARATOR.length()));
            return result.toString();
        }

        private void dump(Map<String, Object> tree, int indent, boolean emptyLine) {
            sort(tree).forEach(e -> {
                result.append(addOffsets("", indent)).append(e.getKey());
                dumpValue(e.getValue(), indent + OFFSET);

                if (addEmptyLineAfterSections && emptyLine) {
                    result.append(LINES_SEPARATOR);
                }
            });
        }

        @SuppressWarnings("unchecked")
        private void dumpValue(Object value, int indent) {
            if (value instanceof Map) {
                result.append(':').append(LINES_SEPARATOR);
                dump((Map<String, Object>) value, indent, false);
                return;
            }
            result.append(": ")
                    .append(value)
                    .append(LINES_SEPARATOR);
        }

        private List<Entry<String, Object>> sort(Map<String, Object> original) {
            return original
                    .entrySet()
                    .stream()
                    .sorted(comparing(this::byDepth).thenComparing(Entry::getKey))
                    .collect(toList());
        }

        private int byDepth(Entry<String, Object> entry) {
            return entry.getValue() instanceof Map ? 1 : 0;
        }
    }
}
