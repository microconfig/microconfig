package io.microconfig.core.properties.io.selector;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.io.ConfigIo;
import io.microconfig.core.properties.io.ConfigReader;
import io.microconfig.core.properties.io.ConfigWriter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;

@RequiredArgsConstructor
public class ConfigIoSelector implements ConfigIo {
    private final ConfigFormatDetector configFormatDetector;

    private final ConfigIo yamlFormat;
    private final ConfigIo propertiesFormat;

    @Override
    public ConfigReader readFrom(File file) {
        return select(file).readFrom(file);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return select(file).writeTo(file);
    }

    private ConfigIo select(File file) {
        ConfigFormat format = configFormatDetector.detectConfigFormat(file);
        if (format == YAML) return yamlFormat;
        if (format == PROPERTIES) return propertiesFormat;

        throw new IllegalStateException("Unsupported format " + format + " for " + file);
    }
}