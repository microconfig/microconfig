package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.composite;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(List<PlaceholderResolveStrategy> strategies) {
        return new CompositeResolveStrategy(strategies);
    }

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        return strategies.stream()
                .map(s -> s.resolve(placeholder))
                .map(Optional::get)
                .findFirst();
    }
}