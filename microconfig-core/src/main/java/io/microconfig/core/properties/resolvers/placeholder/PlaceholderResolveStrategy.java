package io.microconfig.core.properties.resolvers.placeholder;

import io.microconfig.core.properties.Property;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(String component, String key, String environment, String configType);
}