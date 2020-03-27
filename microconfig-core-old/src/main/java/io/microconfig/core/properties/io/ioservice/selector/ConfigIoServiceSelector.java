package io.microconfig.core.properties.io.ioservice.selector;

import io.microconfig.core.properties.io.ioservice.ConfigIoService;
import io.microconfig.core.properties.io.ioservice.ConfigReader;
import io.microconfig.core.properties.io.ioservice.ConfigWriter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.core.properties.io.ioservice.selector.FileFormat.PROPERTIES;
import static io.microconfig.core.properties.io.ioservice.selector.FileFormat.YAML;

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
        FileFormat format = configFormatDetector.detectConfigFormat(file);
        if (format == YAML) return yamlFormat;
        if (format == PROPERTIES) return propertiesFormat;

        throw new IllegalStateException("Unsupported format " + format + " for " + file);
    }
}