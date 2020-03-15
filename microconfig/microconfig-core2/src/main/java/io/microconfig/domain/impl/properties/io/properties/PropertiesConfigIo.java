package io.microconfig.domain.impl.properties.io.properties;

import io.microconfig.domain.impl.properties.io.ConfigIo;
import io.microconfig.domain.impl.properties.io.ConfigReader;
import io.microconfig.domain.impl.properties.io.ConfigWriter;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class PropertiesConfigIo implements ConfigIo {
    private final FsReader fileFsReader;

    @Override
    public ConfigReader readFrom(File file) {
        return new PropertiesReader(file, fileFsReader);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new PropertiesWriter(file);
    }
}