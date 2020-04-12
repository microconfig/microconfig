package io.microconfig.core.properties.resolvers.placeholder.strategies.component.properties;

import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class NameProperty implements ComponentProperty {
    @Override
    public String key() {
        return "name";
    }

    @Override
    public Optional<String> resolveFor(String component, String __) {
        return of(component);
    }
}
