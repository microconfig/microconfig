package io.microconfig.core.properties.impl.repository;

import io.microconfig.core.properties.impl.io.ConfigIo;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class ConfigFileParserImpl implements ConfigFileParser {
    private final ConfigIo configIo;

    @Override
    public ConfigDefinition parse(File file, String configType, String env) {
        return new ConfigFile(file, configType, env).parseUsing(configIo);
    }
}