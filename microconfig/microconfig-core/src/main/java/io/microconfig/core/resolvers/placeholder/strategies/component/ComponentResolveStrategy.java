package io.microconfig.core.resolvers.placeholder.strategies.component;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.impl.DeclaringComponentImpl;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.impl.PropertyImpl.property;
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