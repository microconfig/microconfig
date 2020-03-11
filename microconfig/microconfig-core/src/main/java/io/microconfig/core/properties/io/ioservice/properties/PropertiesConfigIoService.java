package io.microconfig.core.properties.io.ioservice.properties;

import io.microconfig.core.properties.io.io.Io;
import io.microconfig.core.properties.io.ioservice.ConfigIoService;
import io.microconfig.core.properties.io.ioservice.ConfigReader;
import io.microconfig.core.properties.io.ioservice.ConfigWriter;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class PropertiesConfigIoService implements ConfigIoService {
    private final Io io;

    @Override
    public ConfigReader read(File file) {
        return new PropertiesReader(file, io);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new PropertiesWriter(file);
    }
}