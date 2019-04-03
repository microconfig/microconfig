package io.microconfig.configs.resolver.placeholder;

public interface StrategySelector {
    PlaceholderResolveStrategy selectStrategy(Placeholder placeholder);
}
