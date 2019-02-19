package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;
import io.microconfig.properties.Property.Source;
import io.microconfig.properties.resolver.placeholder.ResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class MapResolveStrategy implements ResolveStrategy {
    private final String componentName;
    private final Map<String, ?> keyToValue;

    @SuppressWarnings("unchecked")
    public static ResolveStrategy systemPropertiesResolveStrategy() {
        return new MapResolveStrategy("system", new HashMap(System.getProperties()));
    }

    public static ResolveStrategy envVariablesResolveStrategy() {
        return new MapResolveStrategy("env", System.getenv());
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        if (!componentName.equals(component.getName())) return empty();

        return ofNullable(keyToValue.get(key))
                .map(value -> new Property(key, value.toString(), environment, new Source(component, componentName)));
    }
}