package io.microconfig.core.properties.io.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.addOffsets;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class YamlTreeImpl implements YamlTree {
    private static final int OFFSET = 2;
    // a value can have '\n' line breaks on Windows too, e.g. from a placeholder
    private static final Pattern LINE_BREAK = lineBreakFor(LINES_SEPARATOR);
    private final boolean addEmptyLineAfterSections;

    public YamlTreeImpl() {
        this(true);
    }

    public YamlTreeImpl(boolean addEmptyLineAfterSections) {
        this.addEmptyLineAfterSections = addEmptyLineAfterSections;
    }

    // on Unix exactly the separator, so values are split as before; on Windows both '\r\n' and '\n'
    static Pattern lineBreakFor(String lineSeparator) {
        return compile("\n".equals(lineSeparator) ? "\n" : "\r?\n");
    }

    @Override
    public String toYaml(Map<String, String> flatProperties) {
        Map<String, Object> tree = new TreeCreator().toTree(flatProperties);
        return new YamlDumper().toYamlFromTree(tree);
    }

    static class TreeCreator {
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
            int depth = 1; // of the key in the tree: a part that conflicts with a scalar stays in the key, 'section.child'
            for (int i = 0; i < parts.size() - 1; i++) {
                String part = parts.get(i);
                Object oldValue = result.get(part);
                if (oldValue == null) {
                    Map<String, Object> newMap = new TreeMap<>();
                    result.put(part, newMap);
                    result = newMap;
                    ++depth;
                } else if (oldValue instanceof Map) {
                    result = (Map<String, Object>) oldValue;
                    ++depth;
                } else {
                    parts.set(i + 1, part + "." + parts.get(i + 1));
                }
            }

            result.put(parts.get(parts.size() - 1), offsetForMultilineValue(depth, value));
        }

        private String offsetForMultilineValue(int depth, String value) {
            if (value.startsWith("-")) {
                return withOffsets(depth, value);
            }

            if (value.indexOf('\n') >= 0) {
                return "|" + indentationIndicator(value) + withOffsets(depth, value);
            }

            return value;
        }

        // empty lines of the value stay empty, without trailing spaces of the offset
        private String withOffsets(int depth, String value) {
            String offset = addOffsets("", depth * OFFSET);
            return Stream.of(LINE_BREAK.split(value, -1))
                    .map(line -> line.isEmpty() ? line : offset + line)
                    .collect(joining(LINES_SEPARATOR, LINES_SEPARATOR, ""));
        }

        // without the indicator YAML takes the indentation from the first line, so leading spaces of the value would be lost or break the block
        private String indentationIndicator(String value) {
            int firstLineStart = 0;
            while (firstLineStart < value.length() && isLineBreak(value.charAt(firstLineStart))) firstLineStart++;
            return firstLineStart < value.length() && value.charAt(firstLineStart) == ' ' ? String.valueOf(OFFSET) : "";
        }

        private boolean isLineBreak(char c) {
            return c == '\n' || c == '\r';
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
