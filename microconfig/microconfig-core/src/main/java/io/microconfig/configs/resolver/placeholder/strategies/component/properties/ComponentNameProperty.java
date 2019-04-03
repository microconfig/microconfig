package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentProperty;
import io.microconfig.environments.Component;

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
