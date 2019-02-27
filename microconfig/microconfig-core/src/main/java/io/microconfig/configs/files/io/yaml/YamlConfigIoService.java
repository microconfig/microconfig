package io.microconfig.configs.files.io.yaml;

import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.ConfigReader;
import io.microconfig.configs.files.io.ConfigWriter;
import io.microconfig.utils.reader.FileReader;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class YamlConfigIoService implements ConfigIoService {
    private final FileReader fileReader;

    @Override
    public ConfigReader read(File file) {
        return new YamlReader(file, fileReader);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new YamlWriter(file);
    }
}