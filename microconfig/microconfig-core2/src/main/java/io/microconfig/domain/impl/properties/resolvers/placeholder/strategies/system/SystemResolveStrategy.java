package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.system;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

import static io.microconfig.domain.impl.properties.PropertyImpl.tempProperty;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SystemResolveStrategy implements PlaceholderResolveStrategy {
    public static final String ENV_OS_SOURCE = "env";
    public static final String SYSTEM_SOURCE = "system";

    private final String name;
    private final Function<String, ?> keyToValue;

    public static PlaceholderResolveStrategy systemPropertiesResolveStrategy() {
        return new SystemResolveStrategy(SYSTEM_SOURCE, System::getProperty);
    }

    public static PlaceholderResolveStrategy envVariablesResolveStrategy() {
        return new SystemResolveStrategy(ENV_OS_SOURCE, System::getenv);
    }

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        if (!name.equals(placeholder.getComponent())) return empty();

        return ofNullable(keyToValue.apply(placeholder.getValue()))
                .map(Object::toString)
                .map(value -> tempProperty(placeholder.getValue(), value, placeholder.getEnvironment(), null).escapeOnWindows());
    }
}