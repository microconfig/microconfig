package io.microconfig.configs.io.ioservice.properties;

import io.microconfig.configs.Property;
import io.microconfig.configs.io.ioservice.AbstractConfigReader;
import io.microconfig.utils.reader.FilesReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.microconfig.configs.Property.isComment;
import static io.microconfig.configs.sources.FileSource.fileSource;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;

class PropertiesReader extends AbstractConfigReader {
    PropertiesReader(File file, FilesReader fileReader) {
        super(file, fileReader);
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

            result.add(Property.parse(currentLine.toString(), env, fileSource(file, index, false)));
            currentLine.setLength(0);
        }

        return result;
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }
}