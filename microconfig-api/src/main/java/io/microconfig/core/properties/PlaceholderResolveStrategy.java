package io.microconfig.core.properties;

import java.util.Optional;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(String root, String component, String key, String environment, String configType);
}