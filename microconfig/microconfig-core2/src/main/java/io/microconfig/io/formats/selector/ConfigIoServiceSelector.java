package io.microconfig.io.formats.selector;

import io.microconfig.io.formats.ConfigFormat;
import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.ConfigReader;
import io.microconfig.io.formats.ConfigWriter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.io.formats.ConfigFormat.PROPERTIES;
import static io.microconfig.io.formats.ConfigFormat.YAML;

@RequiredArgsConstructor
public class ConfigIoServiceSelector implements ConfigIoService {
    private final ConfigFormatDetector configFormatDetector;

    private final ConfigIoService yamlFormat;
    private final ConfigIoService propertiesFormat;

    @Override
    public ConfigReader read(File file) {
        return select(file).read(file);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return select(file).writeTo(file);
    }

    private ConfigIoService select(File file) {
        ConfigFormat format = configFormatDetector.detectConfigFormat(file);
        if (format == YAML) return yamlFormat;
        if (format == PROPERTIES) return propertiesFormat;

        throw new IllegalStateException("Unsupported format " + format + " for " + file);
    }
}