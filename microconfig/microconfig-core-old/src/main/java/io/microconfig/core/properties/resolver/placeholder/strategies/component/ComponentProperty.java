package io.microconfig.core.properties.resolver.placeholder.strategies.component;

import io.microconfig.core.environments.Component;

import java.util.Optional;

public interface ComponentProperty {
    String key();

    Optional<String> value(Component component);
}
