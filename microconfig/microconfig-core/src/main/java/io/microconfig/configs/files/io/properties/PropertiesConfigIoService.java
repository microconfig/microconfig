package io.microconfig.configs.files.io.properties;

import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.ConfigReader;
import io.microconfig.configs.files.io.ConfigWriter;

import java.io.File;

public class PropertiesConfigIoService implements ConfigIoService {
    @Override
    public ConfigReader read(File file) {
        return new PropertiesReader(file);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new PropertiesWriter(file);
    }
}