package io.microconfig.properties.resolver.placeholder.strategies.component;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.properties.sources.SpecialSource;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.properties.Property.tempProperty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentResolveStrategy implements PlaceholderResolveStrategy {
    private final Map<String, ComponentProperty> properties;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String envName) {
        ComponentProperty componentProperty = properties.get(propertyKey);

        return ofNullable(componentProperty)
                .flatMap(p -> p.value(component))
                .map(value -> tempProperty(propertyKey, value, envName, new SpecialSource(component, "COMPONENT")));
    }
}