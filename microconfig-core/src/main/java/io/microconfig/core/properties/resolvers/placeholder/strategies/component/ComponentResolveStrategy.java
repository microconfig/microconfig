package io.microconfig.core.properties.resolvers.placeholder.strategies.component;

import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.PropertyImpl.property;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentResolveStrategy implements PlaceholderResolveStrategy {
    private final Map<String, ComponentProperty> propertyByKey;

    @Override
    public Optional<Property> resolve(String component, String key, String environment, String configType) {
        ComponentProperty componentProperty = propertyByKey.get(key);

        return ofNullable(componentProperty)
                .flatMap(p -> p.resolveFor(component, environment))
                .map(value -> property(component, value, PROPERTIES, new DeclaringComponentImpl(configType, component, environment)));
    }
}