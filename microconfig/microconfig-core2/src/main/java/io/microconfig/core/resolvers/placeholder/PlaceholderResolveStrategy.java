package io.microconfig.core.resolvers.placeholder;

import io.microconfig.core.properties.Property;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(String configType, String component, String environment, String key);
}
