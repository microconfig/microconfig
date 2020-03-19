package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Components;
import io.microconfig.core.properties.Properties;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.properties.impl.PropertiesImpl.composite;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode
@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public Properties getPropertiesFor(ConfigTypeFilter filter) {
        return composite(
                components.stream()
                        .map(c -> c.getPropertiesFor(filter))
                        .map(Properties::asList)
                        .flatMap(List::stream)
                        .collect(toList())
        );
    }

    @Override
    public String toString() {
        return components.toString();
    }
}