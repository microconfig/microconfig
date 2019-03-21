package io.microconfig.configs.resolver.placeholder.strategies.system;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.configs.sources.SpecialSource;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

import static io.microconfig.configs.Property.tempProperty;
import static io.microconfig.configs.sources.SpecialSource.ENV_OS_SOURCE;
import static io.microconfig.configs.sources.SpecialSource.SYSTEM_SOURCE;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SystemResolveStrategy implements PlaceholderResolveStrategy {
    private final String name;
    private final Function<String, ?> keyToValue;

    public static PlaceholderResolveStrategy systemPropertiesResolveStrategy() {
        return new SystemResolveStrategy(SYSTEM_SOURCE, System::getProperty);
    }

    public static PlaceholderResolveStrategy envVariablesResolveStrategy() {
        return new SystemResolveStrategy(ENV_OS_SOURCE, System::getenv);
    }

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        if (!name.equals(component.getName())) return empty();

        return ofNullable(keyToValue.apply(propertyKey))
                .map(Object::toString)
                .map(value -> tempProperty(propertyKey, value, environment, new SpecialSource(component, name)))
                .map(Property::escapeOnWindows);
    }
}