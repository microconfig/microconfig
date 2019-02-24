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
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

        }

        return result;
    }
}
