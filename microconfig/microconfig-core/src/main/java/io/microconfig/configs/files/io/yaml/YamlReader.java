package io.microconfig.configs.files.io.yaml;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.AbstractConfigReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static io.microconfig.configs.PropertySource.fileSource;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.lang.Character.isWhitespace;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

class YamlReader extends AbstractConfigReader {
    YamlReader(File file) {
        super(file);
    }

    @Override
    public List<Property> properties(String env) {
        List<Property> result = new ArrayList<>();

        Deque<KeyOffset> currentProperty = new ArrayDeque<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (skip(line)) continue;

            int currentOffset = offsetIndex(line);

            if (multilineValue(line, currentOffset)) {
                index = addMultilineValue(result, currentProperty, currentOffset, index, env);
            } else {
                parseSimpleProperty(result, currentProperty, currentOffset, index, env);
            }
        }

        return result;
    }

    private boolean multilineValue(String line, int currentOffset) {
        char c = line.charAt(currentOffset);
        return c == '-' || c == '[' || c == '>';
    }

    private int addMultilineValue(List<Property> result,
                                  Deque<KeyOffset> currentProperty, int currentOffset,
                                  int originalIndex, String env) {
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
            String nextLine = lines.get(index + 1);
            if (!skip(nextLine) && offsetIndex(nextLine) < currentOffset) {
                break;
            }
            value.append(LINES_SEPARATOR);
            ++index;
        }

        addValue(result, currentProperty, currentOffset, originalIndex, null, value.toString(), env);
        return index;
    }

    private void parseSimpleProperty(List<Property> result,
                                     Deque<KeyOffset> currentProperty, int currentOffset,
                                     int index, String env) {
        String line = lines.get(index);
        int separatorIndex = line.indexOf(':', currentOffset);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Property must contain ':'. Bad property: " + separatorIndex + " in " + file);
        }

        removePropertiesWithBiggerOffset(currentProperty, currentOffset);

        String key = line.substring(currentOffset, separatorIndex).trim();

        if (valueEmpty(line, separatorIndex)) {
            if (itsLastProperty(index, currentOffset)) {
                addValue(result, currentProperty, currentOffset, index - 1, key, "", env);
                return;
            }

            currentProperty.add(new KeyOffset(key, currentOffset, index));
            return;
        }

        String value = line.substring(separatorIndex + 1).trim();
        addValue(result, currentProperty, currentOffset, index, key, value, env);
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
            return currentOffset >= offsetIndex(line);
        }

        return true;
    }

    private void addValue(List<Property> result,
                          Deque<KeyOffset> currentProperty, int currentOffset, int line,
                          String lastKey, String value, String env) {
        if (lastKey != null) {
            currentProperty.add(new KeyOffset(lastKey, currentOffset, line));
        }
        int lineNumber = currentProperty.peekFirst().lineNumber;
        String key = toProperty(currentProperty);
        currentProperty.pollLast();

        result.add(new Property(key, value, env, fileSource(file, lineNumber)));
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