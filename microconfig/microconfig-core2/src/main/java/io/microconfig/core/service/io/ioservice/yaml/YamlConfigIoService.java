package io.microconfig.core.service.io.ioservice.yaml;

import io.microconfig.core.service.io.ioservice.ConfigIoService;
import io.microconfig.core.service.io.ioservice.ConfigReader;
import io.microconfig.core.service.io.ioservice.ConfigWriter;
import io.microconfig.utils.reader.Io;
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