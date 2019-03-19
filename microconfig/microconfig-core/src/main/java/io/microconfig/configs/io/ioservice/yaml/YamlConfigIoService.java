package io.microconfig.configs.io.ioservice.yaml;

import io.microconfig.configs.io.ioservice.ConfigIoService;
import io.microconfig.configs.io.ioservice.ConfigReader;
import io.microconfig.configs.io.ioservice.ConfigWriter;
import io.microconfig.utils.reader.ConfigFileReader;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class YamlConfigIoService implements ConfigIoService {
    private final ConfigFileReader fileReader;

    @Override
    public ConfigReader read(File file) {
        return new YamlReader(file, fileReader);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new YamlWriter(file);
    }
}