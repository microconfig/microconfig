package io.microconfig.configs.files.io.selector;

import io.microconfig.utils.reader.FileReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

import static io.microconfig.configs.Property.isComment;
import static io.microconfig.configs.Property.separatorIndex;
import static io.microconfig.configs.files.io.selector.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.io.selector.FileFormat.YAML;

@RequiredArgsConstructor
public class ConfigFormatDetectorImpl implements ConfigFormatDetector {
    private final FileReader fileReader;

    @Override
    public FileFormat detectConfigFormat(File file) {
        if (file.getName().endsWith(PROPERTIES.extension())) return PROPERTIES;
        if (file.getName().endsWith(YAML.extension())) return YAML;
        return hasYamlOffsets(file) ? YAML : PROPERTIES;
    }

    private boolean hasYamlOffsets(File file) {
        if (!file.exists()) return false;

        String firstProperty = fileReader.firstLine(file, containsValue()).orElse(null);
        if (firstProperty == null) return false;

        int separatorIndex = separatorIndex(firstProperty);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Incorrect property " + firstProperty + " in " + file);
        }

        return firstProperty.charAt(separatorIndex) == ':';
    }

    private Predicate<String> containsValue() {
        return line -> {
            String trimmed = line.trim();
            return !trimmed.isEmpty() && !isComment(trimmed);
        };
    }
}