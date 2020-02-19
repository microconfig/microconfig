package io.microconfig.properties.resolver.placeholder;

public interface StrategySelector {
    PlaceholderResolveStrategy selectStrategy(String configType);
}