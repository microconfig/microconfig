package io.microconfig.core.properties.impl.io.selector;

import io.microconfig.core.properties.impl.io.ConfigFormat;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

import static io.microconfig.core.properties.impl.PropertyImpl.findKeyValueSeparatorIndexIn;
import static io.microconfig.core.properties.impl.PropertyImpl.isComment;
import static io.microconfig.core.properties.impl.io.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.impl.io.ConfigFormat.YAML;


@RequiredArgsConstructor
public class ConfigFormatDetectorImpl implements ConfigFormatDetector {
    private final FsReader fileFsReader;

    @Override
    public ConfigFormat detectConfigFormat(File file) {
        if (file.getName().endsWith(PROPERTIES.extension())) return PROPERTIES;
        if (file.getName().endsWith(YAML.extension())) return YAML;
        return hasYamlOffsets(file) ? YAML : PROPERTIES;
    }

    private boolean hasYamlOffsets(File file) {
        if (!file.exists()) return false;

        String firstProperty = fileFsReader.firstLine(file, containsValue()).orElse(null);
        if (firstProperty == null) return false;

        int separatorIndex = findKeyValueSeparatorIndexIn(firstProperty);
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