package io.microconfig.core.resolvers.placeholder.strategies.system;

import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.Placeholder;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.PlaceholderSource;
import io.microconfig.utils.Os;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.microconfig.core.properties.impl.PropertyImpl.tempProperty;
import static io.microconfig.utils.StringUtils.escape;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SystemResolveStrategy implements PlaceholderResolveStrategy {
    public static final String ENV_OS_SOURCE = "env";
    public static final String SYSTEM_SOURCE = "system";

    private final String propertyKey;
    private final UnaryOperator<String> resolver;

    public static PlaceholderResolveStrategy systemPropertiesResolveStrategy() {
        return new SystemResolveStrategy(SYSTEM_SOURCE, System::getProperty);
    }

    public static PlaceholderResolveStrategy envVariablesResolveStrategy() {
        return new SystemResolveStrategy(ENV_OS_SOURCE, System::getenv);
    }

    @Override
    public Optional<Property> resolve(Placeholder p) {
        if (!propertyKey.equals(p.getComponent())) return empty();

        return ofNullable(resolver.apply(p.getValue()))
                .map(v -> escapeOnWindows(v, p.getValue()))
                .map(value -> tempProperty(p.getValue(), value, p.getEnvironment(), new PlaceholderSource(p.getComponent(), p.getComponent())));
    }

    private String escapeOnWindows(String value, String key) {
        if (!Os.isWindows()) return value;
        return ("user.home".equals(key)) ? unixLikePath(value) : escape(value);
    }
}