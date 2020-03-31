package io.microconfig.core.properties.resolvers.placeholder.strategies.composite;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.findFirstResult;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(List<PlaceholderResolveStrategy> strategies) {
        return new CompositeResolveStrategy(strategies);
    }

    @Override
    public Optional<Property> resolve(String component, String key, String environment, String configType) {
        return findFirstResult(strategies, s -> s.resolve(component, key, environment, configType));
    }
}