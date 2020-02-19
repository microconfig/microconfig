package io.microconfig.core.properties.resolver.placeholder;

public interface StrategySelector {
    PlaceholderResolveStrategy selectStrategy(String configType);
}