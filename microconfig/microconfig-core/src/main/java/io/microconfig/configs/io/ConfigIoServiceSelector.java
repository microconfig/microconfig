package io.microconfig.configs.io;

import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class ConfigIoServiceSelector implements ConfigIoService {
    private final ConfigIoService yamlFormat;
    private final ConfigIoService propertiesFormat;

    @Override
    public ConfigReader read(File file) {
        return select(file).read(file);
    }

    @Override
    public ConfigWriter writeTo(File file) {
        return select(file).writeTo(file);
    }

    private ConfigIoService select(File file) {
        return file.getName().endsWith(".yaml") ? yamlFormat : propertiesFormat;
    }
}