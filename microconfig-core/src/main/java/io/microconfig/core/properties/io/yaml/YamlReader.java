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
import static io.microconfig.core.properties.PropertyImpl.isComment;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.lang.Character.isWhitespace;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

class YamlReader extends AbstractConfigReader {
    YamlReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
    }

    @Override
    protected List<Property> properties(String configType, String environment, boolean __) {
        List<Property> result = new ArrayList<>();

        Deque<KeyOffset> currentProperty = new ArrayDeque<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (skip(line)) continue;

            int currentOffset = offsetIndex(line);

            if (isMultilineValue(line, currentOffset)) {
                index = addMultilineValue(result, currentProperty, currentOffset, index, configType, environment);
            } else {
                parseSimpleProperty(result, currentProperty, currentOffset, index, configType, environment);
            }
        }

        return result;
    }

    private boolean isMultilineValue(String line, int currentOffset) {
        char c = line.charAt(currentOffset);
        return c == '-' || c == '[' || c == '>'
                || (c == '$' && line.length() > currentOffset + 1 && line.charAt(currentOffset + 1) == '{');
    }

    private int addMultilineValue(List<Property> result,
                                  Deque<KeyOffset> currentProperty, int currentOffset,
                                  int originalIndex, String configType, String env) {
        StringBuilder value = new StringBuilder();
        int index = originalIndex;
        while (true) {
            String line = lines.get(index);
            if (!line.isEmpty()) {
                value.append(line.substring(currentOffset));
            }
            if (index + 1 >= lines.size()) {
                break;
            }
            if (multilineValueEnd(lines.get(index + 1), currentOffset)) {
                break;
            }

            value.append(LINES_SEPARATOR);
            ++index;
        }

        addValue(result, currentProperty, currentOffset, originalIndex, null, value.toString(), configType, env);
        return index;
    }

    private boolean multilineValueEnd(String nextLine, int currentOffset) {
        if (skip(nextLine) && !isComment(nextLine)) return false;

        int nextOffset = offsetIndex(nextLine);
        if (currentOffset > nextOffset) return true;
        return currentOffset == nextOffset && !isMultilineValue(nextLine, nextOffset);
    }

    private void parseSimpleProperty(List<Property> result,
                                     Deque<KeyOffset> currentProperty, int currentOffset,
                                     int index, String configType, String env) {
        String line = lines.get(index);
        int separatorIndex = line.indexOf(':', currentOffset);
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

    private boolean valueEmpty(String line, int separatorIndex) {
        return line.substring(separatorIndex + 1).trim().isEmpty();
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
                return !isMultilineValue(line, offsetIndex);
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
        String key = toProperty(currentProperty);
        currentProperty.pollLast();

        result.add(property(key, value, YAML, fileSource(file, lineNumber, true, configType, env)));
    }

    private String toProperty(Deque<KeyOffset> currentProperty) {
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