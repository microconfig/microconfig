package io.microconfig.configs.resolver.placeholder.strategies.standard;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.ResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class StandardResolveStrategy implements ResolveStrategy {
    private final ConfigProvider configProvider;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        return ofNullable(configProvider.getProperties(component, environment).get(propertyKey));
    }
}