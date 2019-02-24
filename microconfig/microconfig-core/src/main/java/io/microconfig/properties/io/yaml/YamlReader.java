package io.microconfig.properties.io.yaml;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.IoUtils.readAllLines;
import static java.lang.Character.isWhitespace;
import static java.util.stream.Collectors.joining;

public class YamlReader {
    public Map<String, String> readAsFlatMap(File file) {
        Map<String, String> result = new TreeMap<>();

        Deque<KeyOffset> currentProperty = new ArrayDeque<>();
        for (String line : readAllLines(file)) {

            if (line.isEmpty() || line.startsWith("#")) continue;

            int currentOffset = offsetIndex(line);
            if (newPropertyStart(currentOffset, currentProperty)) {
                setNullValueForLastKey(currentProperty, currentOffset, result);
            }

            int separatorIndex = line.indexOf(':', currentOffset);
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Property must contain ':'. Bad property: " + separatorIndex + " in " + file);
            }

            String key = line.substring(currentOffset, separatorIndex).trim();

            if (separatorIndex == line.length() - 1) {
                currentProperty.add(new KeyOffset(currentOffset, key));
                continue;
            }

            String value = line.substring(separatorIndex + 1).trim();
            addValue(result, currentProperty, currentOffset, key, value);
        }

        return result;
    }

    private boolean newPropertyStart(int currentOffset, Deque<KeyOffset> currentProperty) {
        return currentProperty.isEmpty() || currentProperty.peekLast().offset >= currentOffset;
    }

    private void setNullValueForLastKey(Deque<KeyOffset> currentProperty, int currentOffset, Map<String, String> result) {
        if (!currentProperty.isEmpty() && currentProperty.peekLast().offset >= currentOffset) {
            result.put(toProperty(currentProperty), "");

            while (!currentProperty.isEmpty() && currentProperty.pollLast().offset >= currentOffset) {
                currentProperty.pollLast();
            }
        }
    }

    private void addValue(Map<String, String> result, Deque<KeyOffset> currentProperty, int currentOffset, String lastKey, String value) {
        currentProperty.add(new KeyOffset(currentOffset, lastKey));
        String key = toProperty(currentProperty);
        currentProperty.pollLast();

        result.put(key, value);
    }

    private int offsetIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!isWhitespace(line.charAt(i))) return i;
        }

        throw new IllegalStateException("assertion error: line is empy");
    }

    private String toProperty(Deque<KeyOffset> currentProperty) {
        return currentProperty.stream()
                .map(k -> k.value)
                .collect(joining("."));
    }

    @RequiredArgsConstructor
    private static class KeyOffset {
        private final int offset;
        private final String value;
    }
}