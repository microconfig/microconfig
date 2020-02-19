package io.microconfig.properties.resolver.placeholder.strategies.component;

import io.microconfig.environments.Component;

import java.util.Optional;

public interface ComponentProperty {
    String key();

    Optional<String> value(Component component);
}
