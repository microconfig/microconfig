package io.microconfig.io.formats.properties;

import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.ConfigReader;
import io.microconfig.io.formats.ConfigWriter;
import io.microconfig.io.formats.Io;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class PropertiesConfigIoService implements ConfigIoService {
    private final Io fileReader;

    @Override
    public ConfigReader readFrom(File file) {
        return new PropertiesReader(file, fileReader);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new PropertiesWriter(file);
    }
}