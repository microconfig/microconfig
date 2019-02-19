package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.ResolverStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class CompositeResolverStrategy implements ResolverStrategy {
    private final List<ResolverStrategy> strategies;

    public static ResolverStrategy composite(ResolverStrategy... strategies) {
        return new CompositeResolverStrategy(asList(strategies));
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        for (ResolverStrategy strategy : strategies) {
            Optional<Property> value = strategy.resolve(key, component, environment);
            if (value.isPresent()) return value;
        }

        return empty();
    }
}