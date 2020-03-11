package io.microconfig.domain.impl.environment;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.ConfigBuildResultsImpl;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigBuildResult> results = components.stream()
                .flatMap(c -> c.buildPropertiesFor(filter).asList().stream())
                .collect(toList());

        return new ConfigBuildResultsImpl(results);
    }
}