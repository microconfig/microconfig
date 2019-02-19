package io.microconfig.properties.resolver.placeholder;

import io.microconfig.environments.Component;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.PropertyFetcher;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SimplePropertyFetcher implements PropertyFetcher {
    private final PropertiesProvider propertiesProvider;

    @Override
    public Optional<Property> getProperty(String key, Component component, String environment) {
        Map<String, Property> properties = propertiesProvider.getProperties(component, environment);
        return ofNullable(properties.get(key));
    }
}