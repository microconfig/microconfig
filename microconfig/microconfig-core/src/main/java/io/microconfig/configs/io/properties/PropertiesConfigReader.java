package io.microconfig.configs.io.properties;

import io.microconfig.configs.Property;
import io.microconfig.configs.io.ConfigReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.IoUtils.readAllLines;

@RequiredArgsConstructor
class PropertiesConfigReader implements ConfigReader {
    private final File file;
    private final List<String> lines;

    PropertiesConfigReader(File file) {
        this(file, readAllLines(file));
    }

    @Override
    public List<Property> properties() {
        return null;
    }

    @Override
    public List<String> comments() {
        return null;
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> keyToValue = new LinkedHashMap<>();
        StringBuilder lastLine = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || isComment(trimmed)) continue;

            lastLine.append(trimmed);
            if (isMultilineValue(trimmed)) {
                lastLine.append(LINES_SEPARATOR);
                continue;
            }

            int separatorIndex = separatorIndex(lastLine);
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Property must contain '=' or ':'. Bad property: " + trimmed + " in " + file);
            }
            String key = lastLine.substring(0, separatorIndex);
            String value = lastLine.substring(separatorIndex + 1);
            keyToValue.put(key, value);

            lastLine.setLength(0);
        }

        return keyToValue;
    }

    private boolean isComment(String trimmed) {
        return trimmed.startsWith("#");
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }

    private int separatorIndex(StringBuilder line) {
        int eqIndex = line.indexOf("=");
        if (eqIndex < 0) return line.indexOf(":");

        int colonIndex = line.lastIndexOf(":", eqIndex - 1);
        return colonIndex < 0 ? eqIndex : colonIndex;
    }
}
