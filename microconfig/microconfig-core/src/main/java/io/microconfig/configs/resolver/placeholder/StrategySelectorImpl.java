package io.microconfig.configs.resolver.placeholder;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class StrategySelectorImpl implements StrategySelector {
    private final PlaceholderResolveStrategy defaultStrategy;
    private final Map<String, PlaceholderResolveStrategy> strategyByType;

    @Override
    public PlaceholderResolveStrategy selectStrategy(Placeholder placeholder) {
        Optional<String> type = placeholder.getType();
        if (!type.isPresent()) return defaultStrategy;

        PlaceholderResolveStrategy strategy = strategyByType.get(type.get());
        if (strategy != null) return strategy;
        
        throw new IllegalStateException("Unsupported config type '" + type.get() + "'. Configured types: " + strategyByType.keySet());
    }
}
