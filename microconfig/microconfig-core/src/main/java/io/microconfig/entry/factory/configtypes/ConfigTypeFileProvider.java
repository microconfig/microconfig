package io.microconfig.entry.factory.configtypes;

import io.microconfig.entry.factory.ConfigType;
import io.microconfig.entry.factory.ConfigsTypeProvider;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class ConfigTypeFileProvider implements ConfigsTypeProvider {
    private static final String MICROCONFIG_SETTINGS = "microconfig.yaml";

    @Override
    public List<ConfigType> getConfigTypes(File rootDir) {
        File file = configFile(rootDir);
        return file.exists() ? parse(file) : emptyList();
    }

    private File configFile(File rootDir) {
        return new File(rootDir, MICROCONFIG_SETTINGS);
    }

    private List<ConfigType> parse(File file) {
        return null;
    }
}