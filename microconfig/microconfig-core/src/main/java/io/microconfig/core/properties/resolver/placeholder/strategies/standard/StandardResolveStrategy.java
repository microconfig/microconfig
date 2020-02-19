package io.microconfig.core.properties.resolver.placeholder.strategies.standard;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolveStrategy;
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