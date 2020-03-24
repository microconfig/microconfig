package io.microconfig.core.resolvers.placeholder.strategies;

import io.microconfig.core.properties.PropertySource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class PlaceholderSource implements PropertySource {
    private final String configType;
    private final String component;
    private final String environment;

    @Override
    public String toString() {
        return component + "[" + environment + "]";
    }
}