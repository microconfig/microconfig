package io.microconfig.core.properties.impl.io.properties;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.impl.PropertyImpl;
import io.microconfig.core.properties.impl.io.AbstractConfigReader;
import io.microconfig.io.FsReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.microconfig.core.properties.impl.FilePropertySource.fileSource;
import static io.microconfig.core.properties.impl.PropertyImpl.isComment;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;

class PropertiesReader extends AbstractConfigReader {
    PropertiesReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
    }

    @Override
    protected List<Property> properties(String env, boolean resolveEscape) {
        List<Property> result = new ArrayList<>();

        StringBuilder currentLine = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
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

            Property property = PropertyImpl.parse(currentLine.toString(), env, fileSource(file, index, false));
            result.add(property);
            currentLine.setLength(0);
        }

        return result;
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }
}