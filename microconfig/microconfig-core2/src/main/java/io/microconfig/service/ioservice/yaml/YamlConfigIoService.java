package io.microconfig.service.ioservice.yaml;

import io.microconfig.service.io.Io;
import io.microconfig.service.ioservice.ConfigIoService;
import io.microconfig.service.ioservice.ConfigReader;
import io.microconfig.service.ioservice.ConfigWriter;
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