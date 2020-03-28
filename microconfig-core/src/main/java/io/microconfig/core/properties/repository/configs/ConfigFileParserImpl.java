package io.microconfig.core.properties.repository.configs;

import io.microconfig.core.properties.io.ConfigIo;
import io.microconfig.core.properties.repository.ConfigFileParser;
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