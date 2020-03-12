package io.microconfig.service.io.selector;

import io.microconfig.service.io.ConfigFormat;
import io.microconfig.service.io.ConfigIoService;
import io.microconfig.service.io.ConfigReader;
import io.microconfig.service.io.ConfigWriter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.service.io.ConfigFormat.PROPERTIES;
import static io.microconfig.service.io.ConfigFormat.YAML;

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