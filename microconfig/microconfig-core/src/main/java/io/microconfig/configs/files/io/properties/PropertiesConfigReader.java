package io.microconfig.configs.files.io.properties;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.ConfigReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.configs.Property.filterComments;
import static io.microconfig.configs.Property.isComment;
import static io.microconfig.configs.PropertySource.fileSource;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.IoUtils.readAllLines;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
class PropertiesConfigReader implements ConfigReader {
    private final File file;
    private final List<String> lines;

    PropertiesConfigReader(File file) {
        this(file, readAllLines(file));
    }

    @Override
    public List<Property> properties() {
        return new ArrayList<>(parse().values());
    }

    @Override
    public Map<String, String> propertiesAsMap() {
        return parse()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, p -> p.getValue().getValue()));
    }

    @Override
    public List<String> comments() {
        return filterComments(lines);
    }

    private Map<String, Property> parse() {
        Map<String, Property> keyToValue = new LinkedHashMap<>();
        StringBuilder lastLine = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
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

            boolean temp = false;
            keyToValue.put(key, new Property(key, value, "", temp, fileSource(file, index)));

            lastLine.setLength(0);
        }

        return keyToValue;
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
