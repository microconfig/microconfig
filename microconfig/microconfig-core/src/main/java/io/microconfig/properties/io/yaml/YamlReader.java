package io.microconfig.properties.io.yaml;

import io.microconfig.utils.IoUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class YamlReader {
    public Map<String, String> readAsFlatMap(File file) {
        List<String> lines = IoUtils.readAllLines(file);
        Map<String, String> result = new TreeMap<>();

        StringBuilder key = new StringBuilder();
        for (String str : lines) {
            String line = str.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

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
        }

        return result;
    }
}