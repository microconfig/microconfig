package io.microconfig.configs.properties.resolver;

import io.microconfig.configs.environment.Component;
import io.microconfig.configs.properties.Property;

import java.util.Optional;

public interface PropertyFetcher {
    Optional<Property> getProperty(String key, Component component, String environment);
}
