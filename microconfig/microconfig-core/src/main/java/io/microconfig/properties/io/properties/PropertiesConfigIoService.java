package io.microconfig.properties.io.properties;

import io.microconfig.properties.io.ConfigIoService;
import io.microconfig.properties.io.ConfigReader;
import io.microconfig.properties.io.ConfigWriter;

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