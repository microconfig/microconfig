package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Components;
import io.microconfig.core.properties.CompositeComponentProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.properties.impl.CompositeComponentPropertiesImpl.composite;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter) {
        return composite(
                components.stream()
                        .map(c -> c.getPropertiesFor(filter))
                        .map(CompositeComponentProperties::asList)
                        .flatMap(List::stream)
                        .collect(toList())
        );
    }

    @Override
    public String toString() {
        return components.toString();
    }
}