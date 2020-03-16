package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component.properties;


import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component.ComponentProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class ComponentNameProperty implements ComponentProperty {
    @Override
    public String key() {
        return "name";
    }

    @Override
    public Optional<String> value(String componentName, String __) {
        return of(componentName);
    }
}
