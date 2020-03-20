package io.microconfig.core.resolvers.placeholder.strategies.composite;

import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.findFirstResult;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositeResolveStrategy implements PlaceholderResolveStrategy {
    private final List<PlaceholderResolveStrategy> strategies;

    public static PlaceholderResolveStrategy composite(PlaceholderResolveStrategy... strategies) {
        return new CompositeResolveStrategy(asList(strategies));
    }

    @Override
    public Optional<Property> resolve(String component, String key, String environment, String configType) {
        return findFirstResult(strategies, s -> s.resolve(component, key, environment, configType));
    }
}