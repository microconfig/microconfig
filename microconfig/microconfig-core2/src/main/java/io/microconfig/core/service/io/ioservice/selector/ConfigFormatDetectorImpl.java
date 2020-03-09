package io.microconfig.core.service.io.ioservice.selector;

import io.microconfig.utils.reader.Io;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

import static io.microconfig.core.domain.impl.properties.PropertyImpl.isComment;
import static io.microconfig.core.service.io.ioservice.selector.FileFormat.PROPERTIES;
import static io.microconfig.core.service.io.ioservice.selector.FileFormat.YAML;
import static java.util.stream.IntStream.range;


@RequiredArgsConstructor
public class ConfigFormatDetectorImpl implements ConfigFormatDetector {
    private final Io fileReader;

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

    //todo
    private static int separatorIndex(String keyValue) {
        return range(0, keyValue.length())
                .filter(i -> {
                    char c = keyValue.charAt(i);
                    return c == '=' || c == ':';
                })
                .findFirst()
                .orElse(-1);
    }
}