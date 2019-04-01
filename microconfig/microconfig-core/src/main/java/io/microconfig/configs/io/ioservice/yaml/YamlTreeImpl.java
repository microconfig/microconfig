package io.microconfig.configs.io.ioservice.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.addOffsets;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;

public class YamlTreeImpl implements YamlTree {
    private static final int OFFSET = 2;

    @Override
    public String toYaml(Map<String, String> flatProperties) {
        return toYamlFromTree(toTree(flatProperties));
    }

    private String toYamlFromTree(Map<String, Object> tree) {
        StringBuilder result = new StringBuilder();
        dump(result, tree, 0, true);
        return result.toString();
    }

    Map<String, Object> toTree(Map<String, String> flatProperties) {
        if (!(flatProperties instanceof TreeMap) || ((TreeMap) flatProperties).comparator() != null) {
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

    private String offsetForMultilineValue(int parts, String value) {
        if (!value.startsWith("-")) return value;

        return (LINES_SEPARATOR + value)
                .replace(LINES_SEPARATOR, addOffsets(LINES_SEPARATOR, parts * OFFSET));
    }

    private void dump(StringBuilder result, Map<String, Object> tree, int indent, boolean emptyLine) {
        tree.forEach((key, value) -> {
            result.append(addOffsets("", indent)).append(key);
            dumpValue(result, value, indent + OFFSET);

            if (emptyLine) {
                result.append(LINES_SEPARATOR);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void dumpValue(StringBuilder result, Object value, int indent) {
        if (value instanceof Map) {
            result.append(':').append(LINES_SEPARATOR);
            dump(result, (Map) value, indent, false);
            return;
        }

        result.append(": ")
                .append(value)
                .append(LINES_SEPARATOR);
    }
}
