package io.microconfig.core.properties;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(Placeholder placeholder);
}