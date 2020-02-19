package io.microconfig.core.properties.resolver.placeholder;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.Property;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(Component component, String propertyKey, String environment);
}