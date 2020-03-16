package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.system;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.microconfig.domain.impl.properties.PropertyImpl.tempProperty;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SystemResolveStrategy implements PlaceholderResolveStrategy {
    public static final String ENV_OS_SOURCE = "env";
    public static final String SYSTEM_SOURCE = "system";

    private final String name;
    private final UnaryOperator<String> resolver;

    public static PlaceholderResolveStrategy systemPropertiesResolveStrategy() {
        return new SystemResolveStrategy(SYSTEM_SOURCE, System::getProperty);
    }

    public static PlaceholderResolveStrategy envVariablesResolveStrategy() {
        return new SystemResolveStrategy(ENV_OS_SOURCE, System::getenv);
    }

    @Override
    public Optional<Property> resolve(Placeholder p) {
        if (!name.equals(p.getComponent())) return empty();

        return ofNullable(resolver.apply(p.getValue()))
                .map(value -> tempProperty(p.getValue(), value, p.getEnvironment(), null).escapeOnWindows());
    }
}