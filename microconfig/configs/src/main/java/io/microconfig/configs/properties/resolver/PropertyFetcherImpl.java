package io.microconfig.configs.properties.resolver;

import io.microconfig.configs.environment.Component;
import io.microconfig.configs.properties.PropertiesProvider;
import io.microconfig.configs.properties.Property;
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