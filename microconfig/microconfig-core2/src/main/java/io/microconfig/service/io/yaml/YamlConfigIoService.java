package io.microconfig.service.io.yaml;

import io.microconfig.service.io.ConfigIoService;
import io.microconfig.service.io.ConfigReader;
import io.microconfig.service.io.ConfigWriter;
import io.microconfig.service.io.Io;
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