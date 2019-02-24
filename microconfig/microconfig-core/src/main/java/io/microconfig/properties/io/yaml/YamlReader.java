package io.microconfig.properties.io.yaml;

import io.microconfig.utils.IoUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Character.isWhitespace;

public class YamlReader {
    public Map<String, String> readAsFlatMap(File file) {
        List<String> lines = IoUtils.readAllLines(file);
        Map<String, String> result = new TreeMap<>();

        int lastOffset = -1;
        StringBuilder key = new StringBuilder();
        for (String str : lines) {
            if (str.isEmpty() || str.startsWith("#")) continue;

            int offsetIndex = offsetIndex(str);
            if (offsetIndex > lastOffset) {
                lastOffset = offsetIndex;
            } else {
                result.put(key.toString(), "");
                key.setLength(0);
            }

            String line = str.trim();
            int separatorIndex = line.indexOf(':');
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Property must contain ':'. Bad property: " + separatorIndex + " in " + file);
            }

            key.append(line);
            if (line.length() == separatorIndex) {
                key.append('.');
                continue;
            }

            String value = line.substring(separatorIndex + 1).trim();
            result.put(key.toString(), value);
            lastOffset = -1;
        }

        return result;
    }

    private int offsetIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!isWhitespace(line.charAt(i))) return i;
        }

        throw new IllegalStateException("assertion error: line is empy");
    }
}