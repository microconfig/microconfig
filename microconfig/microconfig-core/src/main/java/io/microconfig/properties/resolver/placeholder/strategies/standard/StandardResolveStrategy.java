package io.microconfig.properties.resolver.placeholder.strategies.standard;

import io.microconfig.environments.Component;
import io.microconfig.properties.ConfigProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final ConfigProvider configProvider;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        return ofNullable(configProvider.getProperties(component, environment).get(propertyKey));
    }
}