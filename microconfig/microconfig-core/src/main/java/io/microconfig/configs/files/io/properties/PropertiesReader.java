package io.microconfig.configs.files.io.properties;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.AbstractConfigReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.microconfig.configs.PropertySource.fileSource;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;

class PropertiesReader extends AbstractConfigReader {
    PropertiesReader(File file) {
        super(file);
    }

    @Override
    public List<Property> properties(String env) {
        List<Property> result = new ArrayList<>();

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

            result.add(Property.parse(currentLine.toString(), env, fileSource(file, index)));
            currentLine.setLength(0);
        }

        return result;
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }
}