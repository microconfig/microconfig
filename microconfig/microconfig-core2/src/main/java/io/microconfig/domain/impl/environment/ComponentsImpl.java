package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.Components;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.ConfigTypeFilter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.domain.impl.properties.ConfigBuildResultsImpl.resultsOf;
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
        return resultsOf(
                components.stream()
                        .map(c -> c.getPropertiesFor(filter))
                        .map(ConfigBuildResults::asList)
                        .flatMap(List::stream)
                        .collect(toList())
        );
    }
}