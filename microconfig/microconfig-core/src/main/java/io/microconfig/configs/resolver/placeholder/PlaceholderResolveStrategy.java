package io.microconfig.configs.resolver.placeholder;

import io.microconfig.configs.Property;
import io.microconfig.environments.Component;

import java.util.Optional;

import static java.util.Optional.empty;

public interface PlaceholderResolveStrategy {
    Optional<Property> resolve(Component component, String propertyKey, String environment);

    static PlaceholderResolveStrategy composite(PlaceholderResolveStrategy... strategies) {
        return (component, propertyKey, environment) -> {
            for (PlaceholderResolveStrategy strategy : strategies) {
                Optional<Property> value = strategy.resolve(component, propertyKey, environment);
                if (value.isPresent()) return value;
            }

            return empty();
        };
    }
}