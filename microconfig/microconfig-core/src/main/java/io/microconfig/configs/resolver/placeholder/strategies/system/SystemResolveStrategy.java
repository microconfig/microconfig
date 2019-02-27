package io.microconfig.configs.resolver.placeholder.strategies.system;

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
public class SystemResolveStrategy implements ResolveStrategy {
    private final String name;
    private final Function<String, ?> keyToValue;

    public static ResolveStrategy systemPropertiesResolveStrategy() {
        return new SystemResolveStrategy("system", System::getProperty);
    }

    public static ResolveStrategy envVariablesResolveStrategy() {
        return new SystemResolveStrategy("env", System::getenv);
    }

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        if (!name.equals(component.getName())) return empty();

        return ofNullable(keyToValue.apply(propertyKey))
                .map(Object::toString)
                .map(value -> tempProperty(propertyKey, value, environment, specialSource(component, name)));
    }
}