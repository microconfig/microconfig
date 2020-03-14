package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Property;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(Placeholder placeholder);
}
