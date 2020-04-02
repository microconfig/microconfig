package io.microconfig.core.properties.io.properties;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.AbstractConfigReader;
import io.microconfig.io.FsReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.PropertyImpl.isComment;
import static io.microconfig.core.properties.PropertyImpl.parse;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;

class PropertiesReader extends AbstractConfigReader {
    PropertiesReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
    }

    @Override
    protected List<Property> properties(String configType, String environment, boolean resolveEscape) {
        List<Property> result = new ArrayList<>();

        StringBuilder currentLine = new StringBuilder();
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || isComment(trimmed)) continue;

            currentLine.append(trimmed);
            if (isMultilineValue(trimmed)) {
                if (resolveEscape) {
                    currentLine.setLength(currentLine.length() - 1);
                } else {
                    currentLine.append(LINES_SEPARATOR);
                }
                continue;
            }

            Property property = parse(currentLine.toString(), PROPERTIES,
                    fileSource(file, lineNumber, false, configType, environment));
            result.add(property);
            currentLine.setLength(0);
        }

        return result;
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }
}