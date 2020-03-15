package io.microconfig.domain.impl.properties.io.yaml;

import io.microconfig.domain.impl.properties.io.ConfigIoService;
import io.microconfig.domain.impl.properties.io.ConfigReader;
import io.microconfig.domain.impl.properties.io.ConfigWriter;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class YamlConfigIoService implements ConfigIoService {
    private final FsReader fileFsReader;

    @Override
    public ConfigReader readFrom(File file) {
        return new YamlReader(file, fileFsReader);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return new YamlWriter(file);
    }
}