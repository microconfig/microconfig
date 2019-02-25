package io.microconfig.configs.io.yaml;

import io.microconfig.configs.io.ConfigIoService;
import io.microconfig.configs.io.ConfigReader;
import io.microconfig.configs.io.ConfigWriter;

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