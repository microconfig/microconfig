package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeConfigTypeProvider implements ConfigTypeProvider {
    private final List<ConfigTypeProvider> providers;

    public static ConfigTypeProvider composite(ConfigTypeProvider... providers) {
        return new CompositeConfigTypeProvider(asList(providers));
    }

    @Override
    public List<ConfigType> getTypes() {
        return providers.stream()
                .map(ConfigTypeProvider::getTypes)
                .filter(types -> !types.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Config types are not configured"));
    }
}