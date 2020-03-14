package io.microconfig.io.formats.yaml;

import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.ConfigReader;
import io.microconfig.io.formats.ConfigWriter;
import io.microconfig.io.formats.Io;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class YamlConfigIoService implements ConfigIoService {
    private final Io fileReader;

    @Override
    public ConfigReader read(File file) {
        return new YamlReader(file, fileReader);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new YamlWriter(file);
    }
}