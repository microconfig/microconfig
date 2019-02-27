package io.microconfig.configs.resolver.placeholder.strategies;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.ResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class StandardResolveStrategy implements ResolveStrategy {
    private final ConfigProvider configProvider;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        Map<String, Property> properties = configProvider.getProperties(component, environment);
        return ofNullable(properties.get(propertyKey));
    }
}