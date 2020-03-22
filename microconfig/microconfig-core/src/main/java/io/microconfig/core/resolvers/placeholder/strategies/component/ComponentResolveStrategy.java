package io.microconfig.core.resolvers.placeholder.strategies.component;

import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.PlaceholderSource;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.core.properties.impl.PropertyImpl.tempProperty;
import static io.microconfig.core.resolvers.placeholder.strategies.PlaceholderSource.COMPONENT_SOURCE;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentResolveStrategy implements PlaceholderResolveStrategy {
    private final Map<String, ComponentProperty> propertyByKey;

    @Override
    public Optional<Property> resolve(String component, String key, String environment, String configType) {
        ComponentProperty componentProperty = propertyByKey.get(key);

        return ofNullable(componentProperty)
                .flatMap(p -> p.resolveFor(component, environment))
                .map(value -> tempProperty(component, value, environment, new PlaceholderSource(component, COMPONENT_SOURCE)));
    }
}