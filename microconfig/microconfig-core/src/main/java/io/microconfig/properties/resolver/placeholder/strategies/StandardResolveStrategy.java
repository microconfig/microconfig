package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.ResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class StandardResolveStrategy implements ResolveStrategy {
    private final PropertiesProvider propertiesProvider;

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        Map<String, Property> properties = propertiesProvider.getProperties(component, environment);
        return ofNullable(properties.get(key));
    }
}