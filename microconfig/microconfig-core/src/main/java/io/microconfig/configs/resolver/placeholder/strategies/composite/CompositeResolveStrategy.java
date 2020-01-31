package io.microconfig.configs.resolver.placeholder.strategies.composite;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(PlaceholderResolveStrategy... strategies) {
        return new CompositeResolveStrategy(asList(strategies));
    }

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        return strategies.stream()
                .map(s -> s.resolve(component, propertyKey, environment))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}