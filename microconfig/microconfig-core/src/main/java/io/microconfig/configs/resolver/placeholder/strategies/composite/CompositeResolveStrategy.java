package io.microconfig.configs.resolver.placeholder.strategies.composite;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(PlaceholderResolveStrategy... strategies) {
        return new CompositeResolveStrategy(asList(strategies));
    }

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        for (PlaceholderResolveStrategy strategy : strategies) {
            Optional<Property> value = strategy.resolve(component, propertyKey, environment);
            if (value.isPresent()) return value;
        }

        return empty();
    }
}