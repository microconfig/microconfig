package io.microconfig.core.configtypes.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeConfigTypeRepository implements ConfigTypeRepository {
    private final List<ConfigTypeRepository> repositories;

    public static ConfigTypeRepository composite(ConfigTypeRepository... repositories) {
        return new CompositeConfigTypeRepository(asList(repositories));
    }

    @Override
    public List<ConfigType> getConfigTypes() {
        return repositories.stream()
                .map(ConfigTypeRepository::getConfigTypes)
                .filter(types -> !types.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Config types are not configured"));
    }
}