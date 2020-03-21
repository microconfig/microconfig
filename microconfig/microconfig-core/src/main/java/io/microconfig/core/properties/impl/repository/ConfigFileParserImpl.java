package io.microconfig.core.properties.impl.repository;

import io.microconfig.core.properties.impl.io.ConfigIo;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class ConfigFileParserImpl implements ConfigFileParser {
    private final ConfigIo configIo;

    @Override
    public ConfigDefinition parse(File file, String env) {
        return new ConfigFile(file, env).parseUsing(configIo);
    }
}