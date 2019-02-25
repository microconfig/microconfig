package io.microconfig.configs.files.io.properties;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.AbstractConfigReader;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.configs.PropertySource.fileSource;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;

class PropertiesConfigReader extends AbstractConfigReader {
    PropertiesConfigReader(File file) {
        super(file);
    }

    @Override
    protected Map<String, Property> parse(String env) {
        Map<String, Property> keyToValue = new LinkedHashMap<>();

        StringBuilder currentLine = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || isComment(trimmed)) continue;

            currentLine.append(trimmed);
            if (isMultilineValue(trimmed)) {
                currentLine.append(LINES_SEPARATOR);
                continue;
            }

            Property property = Property.parse(currentLine.toString(), env, fileSource(file, index));
            keyToValue.put(property.getKey(), property);
            currentLine.setLength(0);
        }

        return keyToValue;
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }
}
