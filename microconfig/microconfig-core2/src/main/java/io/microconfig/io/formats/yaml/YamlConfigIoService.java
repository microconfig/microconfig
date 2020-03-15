package io.microconfig.io.formats.yaml;

import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.ConfigReader;
import io.microconfig.io.formats.ConfigWriter;
import io.microconfig.io.io.FsReader;
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