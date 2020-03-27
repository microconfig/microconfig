package io.microconfig.core.properties.io.selector;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.findSeparatorIndexIn;
import static io.microconfig.core.properties.PropertyImpl.isComment;


@RequiredArgsConstructor
public class ConfigFormatDetectorImpl implements ConfigFormatDetector {
    private final FsReader reader;

    @Override
    public ConfigFormat detectConfigFormat(File file) {
        if (file.getName().endsWith(PROPERTIES.extension())) return PROPERTIES;
        if (file.getName().endsWith(YAML.extension())) return YAML;
        return hasYamlOffsets(file) ? YAML : PROPERTIES;
    }

    private boolean hasYamlOffsets(File file) {
        if (!file.exists()) return false;

        String firstProperty = reader.firstLineOf(file, withValue()).orElse(null);
        if (firstProperty == null) return false;

        int separatorIndex = findSeparatorIndexIn(firstProperty);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Incorrect property " + firstProperty + " in " + file);
        }

        return firstProperty.charAt(separatorIndex) == ':';
    }

    private Predicate<String> withValue() {
        return line -> {
            String trimmed = line.trim();
            return !trimmed.isEmpty() && !isComment(trimmed);
        };
    }
}