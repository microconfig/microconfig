package io.microconfig.configs.io.properties;

import io.microconfig.configs.io.ConfigIoService;
import io.microconfig.configs.io.ConfigReader;
import io.microconfig.configs.io.ConfigWriter;

import java.io.File;

public class PropertiesConfigIoService implements ConfigIoService {
    @Override
    public ConfigReader read(File file) {
        return new PropertiesConfigReader(file);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new PropertiesConfigWriter(file);
    }
}