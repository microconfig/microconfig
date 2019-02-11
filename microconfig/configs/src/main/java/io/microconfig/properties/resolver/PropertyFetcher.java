package io.microconfig.properties.resolver;

import io.microconfig.environment.Component;
import io.microconfig.properties.Property;

import java.util.Optional;

public interface PropertyFetcher {
    Optional<Property> getProperty(String key, Component component, String environment);
}
