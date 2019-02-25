package io.microconfig.properties.io.yaml;

import io.microconfig.properties.io.ConfigIoService;
import io.microconfig.properties.io.ConfigReader;
import io.microconfig.properties.io.ConfigWriter;

import java.io.File;

public class YamlConfigIoService implements ConfigIoService {
    @Override
    public ConfigReader read(File file) {
        return new YamlConfigReader(file);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new YamlConfigWriter(file);
    }
}