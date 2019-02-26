package io.microconfig.configs.files.format;

import java.io.File;
import java.util.stream.Stream;

import static io.microconfig.configs.Property.isComment;
import static io.microconfig.configs.Property.separatorIndex;
import static io.microconfig.configs.files.format.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.format.FileFormat.YAML;
import static io.microconfig.utils.IoUtils.lines;

public class FileFormatDetectorImpl implements FileFormatDetector {
    @Override
    public FileFormat detectFileFormat(File file) {
        if (file.getName().endsWith(PROPERTIES.extension())) return PROPERTIES;
        if (file.getName().endsWith(YAML.extension())) return YAML;
        return hasYamlOffsets(file) ? YAML : PROPERTIES;
    }

    private boolean hasYamlOffsets(File file) {
        if (!file.exists()) return false;

        String firstProperty = firstPropertyLine(file);
        if (firstProperty == null) return false;

        int separatorIndex = separatorIndex(firstProperty);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Incorrect property " + firstProperty + " in " + file);
        }

        return firstProperty.charAt(separatorIndex) == ':';
    }

    private String firstPropertyLine(File file) {
        try (Stream<String> lines = lines(file.toPath())) {
            return lines.map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !isComment(s))
                    .findFirst()
                    .orElse(null);
        }
    }
}