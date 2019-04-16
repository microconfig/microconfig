package io.microconfig.factory.configtypes;

import io.microconfig.factory.ConfigType;
import io.microconfig.factory.ConfigsTypeProvider;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeConfigTypeProvider implements ConfigsTypeProvider {
    private final List<ConfigsTypeProvider> providers;

    public static ConfigsTypeProvider composite(ConfigsTypeProvider... providers) {
        return new CompositeConfigTypeProvider(asList(providers));
    }

    @Override
    public List<ConfigType> getConfigTypes(File rootDir) {
        return providers.stream()
                .map(p -> p.getConfigTypes(rootDir))
                .filter(types -> !types.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Config types are not configured"));
    }
}