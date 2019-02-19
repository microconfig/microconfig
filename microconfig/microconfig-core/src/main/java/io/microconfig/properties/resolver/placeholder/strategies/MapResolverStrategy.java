package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;
import io.microconfig.properties.Property.Source;
import io.microconfig.properties.resolver.placeholder.ResolverStrategy;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class MapResolverStrategy implements ResolverStrategy {
    private final String componentName;
    private final Map<String, ?> keyToValue;

    @SuppressWarnings("unchecked")
    public static ResolverStrategy systemPropertiesResolveStrategy() {
        return new MapResolverStrategy("system", new HashMap(System.getProperties()));
    }

    public static ResolverStrategy envVariablesResolveStrategy() {
        return new MapResolverStrategy("env", System.getenv());
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        if (!component.getName().equals(componentName)) return empty();

        Object value = keyToValue.get(key);
        if (value == null) return empty();

        return of(new Property(key, value.toString(), environment, new Source(component, componentName)));
    }
}