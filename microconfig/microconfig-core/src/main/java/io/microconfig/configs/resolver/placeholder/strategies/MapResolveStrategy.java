package io.microconfig.configs.resolver.placeholder.strategies;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.ResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

import static io.microconfig.configs.Property.tempProperty;
import static io.microconfig.configs.PropertySource.specialSource;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class MapResolveStrategy implements ResolveStrategy {
    private final String name;
    private final Function<String, ?> keyToValue;

    public static ResolveStrategy systemPropertiesResolveStrategy() {
        return new MapResolveStrategy("system", System::getProperty);
    }

    public static ResolveStrategy envVariablesResolveStrategy() {
        return new MapResolveStrategy("env", System::getenv);
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String environment) {
        if (!name.equals(component.getName())) return empty();

        return ofNullable(keyToValue.apply(key))
                .map(Object::toString)
                .map(value -> tempProperty(key, value, environment, specialSource(component, name)));
    }
}