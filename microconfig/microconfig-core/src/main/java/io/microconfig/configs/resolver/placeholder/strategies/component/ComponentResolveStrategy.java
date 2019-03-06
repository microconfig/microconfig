package io.microconfig.configs.resolver.placeholder.strategies.component;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.configs.Property.tempProperty;
import static io.microconfig.configs.PropertySource.specialSource;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentResolveStrategy implements PlaceholderResolveStrategy {
    private final Map<String, ComponentProperty> properties;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String envName) {
        ComponentProperty componentProperty = properties.get(propertyKey);

        return ofNullable(componentProperty)
                .flatMap(p -> p.value(component))
                .map(value -> tempProperty(propertyKey, value, envName, specialSource(component, "COMPONENT")));
    }

    public interface ComponentProperty {
        String key();

        Optional<String> value(Component component);
    }
}