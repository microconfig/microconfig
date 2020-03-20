package io.microconfig.core.properties.impl.io.yaml;

import io.microconfig.core.properties.impl.io.ConfigIo;
import io.microconfig.core.properties.impl.io.ConfigReader;
import io.microconfig.core.properties.impl.io.ConfigWriter;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class YamlConfigIo implements ConfigIo {
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