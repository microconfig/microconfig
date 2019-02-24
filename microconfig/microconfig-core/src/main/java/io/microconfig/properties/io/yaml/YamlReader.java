package io.microconfig.properties.io.yaml;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;

import static io.microconfig.utils.IoUtils.readAllLines;
import static java.lang.Character.isWhitespace;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public class YamlReader {
    public Map<String, String> readAsFlatMap(File file) {
        Map<String, String> result = new LinkedHashMap<>();

        Deque<KeyOffset> currentProperty = new ArrayDeque<>();
        List<String> lines = readAllLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (skip(line)) continue;

            int currentOffset = offsetIndex(line);
            int separatorIndex = line.indexOf(':', currentOffset);
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Property must contain ':'. Bad property: " + separatorIndex + " in " + file);
            }

            removeWithBiggerOffset(currentProperty, currentOffset);

            String key = line.substring(currentOffset, separatorIndex).trim();

            if (isValueEmpty(line, separatorIndex)) {
                if (isLastProperty(lines, i, currentOffset)) {
                    addValue(result, currentProperty, currentOffset, key, "");
                } else {
                    currentProperty.add(new KeyOffset(key, currentOffset));
                }
                continue;
            }

            String value = line.substring(separatorIndex + 1).trim();
            addValue(result, currentProperty, currentOffset, key, value);
        }

        return result;
    }

    private boolean isValueEmpty(String line, int separatorIndex) {
        return separatorIndex == line.length() - 1;
    }

    private void removeWithBiggerOffset(Deque<KeyOffset> currentProperty, int currentOffset) {
        while (!currentProperty.isEmpty() && currentProperty.peekLast().offset >= currentOffset) {
            currentProperty.pollLast();
        }
    }

    private boolean skip(String line) {
        return line.isEmpty() || line.startsWith("#");
    }

    private int offsetIndex(String line) {
        return range(0, line.length())
                .filter(i -> !isWhitespace(line.charAt(i)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("assertion error: line is empty"));
    }

    private boolean isLastProperty(List<String> lines, int i, int currentOffset) {
        ++i;
        while (i < lines.size()) {
            String line = lines.get(i++);
            if (skip(line)) continue;
            return currentOffset >= offsetIndex(line);
        }

        return true;
    }

    private void addValue(Map<String, String> result, Deque<KeyOffset> currentProperty, int currentOffset, String lastKey, String value) {
        currentProperty.add(new KeyOffset(lastKey, currentOffset));
        String key = toProperty(currentProperty);
        currentProperty.pollLast();

        result.put(key, value);
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
    }
}