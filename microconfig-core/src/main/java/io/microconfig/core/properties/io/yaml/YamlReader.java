package io.microconfig.core.properties.io.yaml;

import io.microconfig.core.properties.FileBasedComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.AbstractConfigReader;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.OverrideProperty.isOverrideProperty;
import static io.microconfig.core.properties.OverrideProperty.overrideProperty;
import static io.microconfig.core.properties.PropertyImpl.isComment;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.isBlank;
import static java.lang.Character.isWhitespace;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

class YamlReader extends AbstractConfigReader {
    YamlReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
    }

    @Override
    public List<Property> properties(String configType, String environment) {
        List<Property> result = new ArrayList<>();
        Deque<KeyOffset> currentProperty = new ArrayDeque<>();

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (skip(line)) continue;

            int currentOffset = offsetIndex(line);
            String multiLineKey = multiLineKey(line, currentOffset);
            if (multiLineKey != null) {
                lineNumber = multiLineValue(result, multiLineKey, currentProperty, lineNumber, currentOffset + 2, configType, environment);
            } else if (isComplexValue(line, currentOffset)) {
                lineNumber = addComplexValue(result, currentProperty, currentOffset, lineNumber, configType, environment);
            } else {
                parseSimpleProperty(result, currentProperty, currentOffset, lineNumber, configType, environment);
            }
        }

        return result;
    }

    private String multiLineKey(String line, int currentOffset) {
        int separatorIndex = separatorIndex(line, currentOffset);
        if (separatorIndex < 0 || separatorIndex == line.length() - 1) return null;
        boolean multilinePostfix = line.chars()
                .skip(separatorIndex + 1)
                .filter(c -> !isWhitespace(c))
                .mapToLong(c -> c == '|' ? 1 : 2)
                .sum() == 1;
        return multilinePostfix ? line.substring(0, separatorIndex) : null;
    }

    private int multiLineValue(List<Property> result, String key, Deque<KeyOffset> currentProperty, int index, int offset, String configType, String env) {
        List<String> valueLines = new ArrayList<>();
        int counter = 1;
        while (true) {
            int pointer = index + counter;
            if (pointer >= lines.size()) break;
            String line = lines.get(pointer);
            int currentOffset = line.isEmpty() ? 0 : offsetIndex(line);
            if (currentOffset < offset) break;
            String value = line.substring(offset);
            if (!value.trim().isEmpty()) valueLines.add(line.substring(offset));
            counter++;
        }
        if (valueLines.isEmpty()) {
            throw new IllegalArgumentException("Missing value in multiline key '" + key + "' in '"
                    + new FileBasedComponent(file, index, true, configType, env) + "'");
        }
        FileBasedComponent source = fileSource(file, index, true, configType, env);
        String k = mergeKey(currentProperty, key);
        String v = join(LINES_SEPARATOR, valueLines);
        Property p = isOverrideProperty(k) ? overrideProperty(k, v, YAML, source) : property(k, v, YAML, source);
        result.add(p);
        return index + counter - 1;
    }

    private boolean isComplexValue(String line, int currentOffset) {
        char c = line.charAt(currentOffset);
        return asList('-', '[', ']', '{').contains(c) ||
                (c == '$' && line.length() > currentOffset + 1 && line.charAt(currentOffset + 1) == '{');
    }

    private int addComplexValue(List<Property> result,
                                Deque<KeyOffset> currentProperty, int currentOffset,
                                int originalLineNumber, String configType, String env) {
        StringBuilder value = new StringBuilder();
        int index = originalLineNumber;
        while (true) {
            String line = lines.get(index);
            if (!line.isEmpty()) {
                value.append(line.substring(currentOffset));
            }
            if (index + 1 >= lines.size()) {
                break;
            }
            if (complexValueEnd(lines.get(index + 1), currentOffset)) {
                break;
            }

            value.append(LINES_SEPARATOR);
            ++index;
        }

        addValue(result, currentProperty, currentOffset, originalLineNumber, null, value.toString(), configType, env);
        return index;
    }

    private boolean complexValueEnd(String nextLine, int currentOffset) {
        if (skip(nextLine) && !isComment(nextLine)) return false;

        int nextOffset = offsetIndex(nextLine);
        if (currentOffset > nextOffset) return true;
        return currentOffset == nextOffset && !isComplexValue(nextLine, nextOffset);
    }

    private void parseSimpleProperty(List<Property> result,
                                     Deque<KeyOffset> currentProperty, int currentOffset,
                                     int index, String configType, String env) {
        String line = lines.get(index);
        int separatorIndex = separatorIndex(line, currentOffset);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Incorrect delimiter in '" + line + "' in '" + new FileBasedComponent(file, index, true, configType, env) +
                    "'\nYaml property must contain ':' as delimiter.");
        }

        removePropertiesWithBiggerOffset(currentProperty, currentOffset);

        String key = line.substring(currentOffset, separatorIndex).trim();

        if (valueEmpty(line, separatorIndex)) {
            if (itsLastProperty(index, currentOffset)) {
                addValue(result, currentProperty, currentOffset, index - 1, key, "", configType, env);
                return;
            }

            currentProperty.add(new KeyOffset(key, currentOffset, index));
            return;
        }

        String value = line.substring(separatorIndex + 1).trim();
        addValue(result, currentProperty, currentOffset, index, key, value, configType, env);
    }

    private int separatorIndex(String line, int offset) {
        return line.indexOf(':', offset);
    }

    private boolean valueEmpty(String line, int separatorIndex) {
        return isBlank(line.substring(separatorIndex + 1));
    }

    private void removePropertiesWithBiggerOffset(Deque<KeyOffset> currentProperty, int currentOffset) {
        while (!currentProperty.isEmpty() && currentProperty.peekLast().offset >= currentOffset) {
            currentProperty.pollLast();
        }
    }

    private boolean skip(String line) {
        String trim = line.trim();
        return trim.isEmpty() || isComment(trim);
    }

    private int offsetIndex(String line) {
        return range(0, line.length())
                .filter(i -> !isWhitespace(line.charAt(i)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("assertion error: line is empty"));
    }

    private boolean itsLastProperty(int i, int currentOffset) {
        ++i;
        while (i < lines.size()) {
            String line = lines.get(i++);
            if (skip(line)) continue;
            int offsetIndex = offsetIndex(line);
            if (currentOffset > offsetIndex) {
                return true;
            }
            if (currentOffset == offsetIndex) {
                return !isComplexValue(line, offsetIndex);
            }
            return false;
        }

        return true;
    }

    private void addValue(List<Property> result,
                          Deque<KeyOffset> currentProperty, int currentOffset, int line,
                          String lastKey, String value, String configType, String env) {
        if (lastKey != null) {
            currentProperty.add(new KeyOffset(lastKey, currentOffset, line));
        }
        int lineNumber = currentProperty.peekLast().lineNumber;
        String key = toKey(currentProperty);
        currentProperty.pollLast();
        FileBasedComponent source = fileSource(file, lineNumber, true, configType, env);
        Property prop = isOverrideProperty(key) ? overrideProperty(key, value, YAML, source) : property(key, value, YAML, source);
        result.add(prop);
    }

    private String mergeKey(Deque<KeyOffset> currentProperty, String key) {
        return concat(
                currentProperty.stream().map(k -> k.key),
                of(key.trim())
        ).collect(joining("."));
    }

    private String toKey(Deque<KeyOffset> currentProperty) {
        return currentProperty.stream()
                .map(k -> k.key)
                .collect(joining("."));
    }

    @RequiredArgsConstructor
    private static class KeyOffset {
        private final String key;
        private final int offset;
        private final int lineNumber;

        @Override
        public String toString() {
            return key;
        }
    }
}