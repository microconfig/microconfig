package io.microconfig.configs.resolver.placeholder;

import io.microconfig.configs.Property;
import io.microconfig.environments.Component;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(Component component, String propertyKey, String environment);
}