package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.resolver.placeholder.strategies.component.ComponentProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class ComponentNameProperty implements ComponentProperty {
    @Override
    public String key() {
        return "name";
    }

    @Override
    public Optional<String> value(Component component) {
        return of(component.getName());
    }
}
