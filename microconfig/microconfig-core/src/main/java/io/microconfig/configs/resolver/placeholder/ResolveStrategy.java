package io.microconfig.configs.resolver.placeholder;

import io.microconfig.configs.Property;
import io.microconfig.environments.Component;

import java.util.Optional;

import static java.util.Optional.empty;

public interface ResolveStrategy {
    Optional<Property> resolve(Component component, String propertyKey, String environment);

    static ResolveStrategy composite(ResolveStrategy... strategies) {
        return (component, propertyKey, environment) -> {
            for (ResolveStrategy strategy : strategies) {
                Optional<Property> value = strategy.resolve(component, propertyKey, environment);
                if (value.isPresent()) return value;
            }

            return empty();
        };
    }
}