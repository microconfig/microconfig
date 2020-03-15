package io.microconfig.core.properties.resolver.placeholder.strategies.composite;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.findFirst;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(List<PlaceholderResolveStrategy> strategies) {
        return new CompositeResolveStrategy(strategies);
    }

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        return findFirst(strategies, s -> s.resolve(component, propertyKey, environment));
    }
}