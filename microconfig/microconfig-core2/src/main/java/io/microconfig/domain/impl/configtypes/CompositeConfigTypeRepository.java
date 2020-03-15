package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeConfigTypeRepository implements ConfigTypeRepository {
    private final List<ConfigTypeRepository> repositories;

    public static ConfigTypeRepository composite(ConfigTypeRepository... types) {
        return new CompositeConfigTypeRepository(asList(types));
    }

    @Override
    public List<ConfigType> getRepositories() {
        return repositories.stream()
                .map(ConfigTypeRepository::getRepositories)
                .filter(types -> !types.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Config types are not configured"));
    }
}