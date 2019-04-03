package io.microconfig.configs.resolver.placeholder.strategies.selector;

import io.microconfig.configs.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.configs.resolver.placeholder.StrategySelector;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class StrategySelectorImpl implements StrategySelector {
    private final PlaceholderResolveStrategy defaultStrategy;
    private final Map<String, PlaceholderResolveStrategy> strategyByType;

    @Override
    public PlaceholderResolveStrategy selectStrategy(String configType) {
        if (configType == null) return defaultStrategy;

        PlaceholderResolveStrategy strategy = strategyByType.get(configType);
        if (strategy != null) return strategy;

        throw new IllegalStateException("Unsupported config type '" + configType + "'. Configured types: " + strategyByType.keySet());
    }
}