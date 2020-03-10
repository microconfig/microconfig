package io.microconfig.service.ioservice.selector;

import io.microconfig.service.ioservice.ConfigFormat;
import io.microconfig.service.ioservice.ConfigIoService;
import io.microconfig.service.ioservice.ConfigReader;
import io.microconfig.service.ioservice.ConfigWriter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.service.ioservice.ConfigFormat.PROPERTIES;
import static io.microconfig.service.ioservice.ConfigFormat.YAML;

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