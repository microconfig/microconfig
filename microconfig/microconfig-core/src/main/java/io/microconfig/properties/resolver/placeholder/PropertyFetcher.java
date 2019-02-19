package io.microconfig.properties.resolver.placeholder;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;

import java.util.Optional;

public interface PropertyFetcher {
    Optional<Property> getProperty(String key, Component component, String environment);
}
