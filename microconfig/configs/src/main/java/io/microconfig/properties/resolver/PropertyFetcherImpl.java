package io.microconfig.properties.resolver;

import io.microconfig.environment.Component;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class PropertyFetcherImpl implements PropertyFetcher {
    private final PropertiesProvider propertiesProvider;

    @Override
    public Optional<Property> getProperty(String key, Component component, String environment) {
        Map<String, Property> properties = propertiesProvider.getProperties(component, environment);
        return ofNullable(properties.get(key));
    }
}