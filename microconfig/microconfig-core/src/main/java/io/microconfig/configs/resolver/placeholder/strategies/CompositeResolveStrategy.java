package io.microconfig.configs.resolver.placeholder.strategies;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.ResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements ResolveStrategy {
    private final List<ResolveStrategy> strategies;

    public static ResolveStrategy composite(ResolveStrategy... strategies) {
        return new CompositeResolveStrategy(asList(strategies));
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        for (ResolveStrategy strategy : strategies) {
            Optional<Property> value = strategy.resolve(key, component, environment);
            if (value.isPresent()) return value;
        }

        return empty();
    }
}