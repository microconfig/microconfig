package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;
import io.microconfig.properties.Property.Source;
import io.microconfig.properties.resolver.placeholder.ResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class MapResolveStrategy implements ResolveStrategy {
    private final String componentName;
    private final Function<String, Object> keyToValue;

    public static ResolveStrategy systemPropertiesResolveStrategy() {
        return new MapResolveStrategy("system", System::getProperty);
    }

    public static ResolveStrategy envVariablesResolveStrategy() {
        return new MapResolveStrategy("env", System::getenv);
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        if (!componentName.equals(component.getName())) return empty();

        return ofNullable(keyToValue.apply(key))
                .map(Object::toString)
                .map(value -> new Property(key, value, environment, new Source(component, componentName)));
    }
}