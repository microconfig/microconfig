package io.microconfig.configs.files.io.yaml;

import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.ConfigReader;
import io.microconfig.configs.files.io.ConfigWriter;

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