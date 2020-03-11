package io.microconfig.domain.impl.configtype;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypes;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeConfigTypes implements ConfigTypes {
    private final List<ConfigTypes> providers;

    public static ConfigTypes composite(ConfigTypes... types) {
        return new CompositeConfigTypes(asList(types));
    }

    @Override
    public List<ConfigType> getTypes() {
        return providers.stream()
                .map(ConfigTypes::getTypes)
                .filter(types -> !types.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Config types are not configured"));
    }
}