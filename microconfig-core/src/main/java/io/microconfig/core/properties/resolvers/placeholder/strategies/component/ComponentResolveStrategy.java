package io.microconfig.core.properties.resolvers.placeholder.strategies.component;

import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.Placeholder;
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
    public Optional<Property> resolve(Placeholder placeholder) {
        ComponentProperty componentProperty = propertyByKey.get(placeholder.getKey());

        return ofNullable(componentProperty)
                .flatMap(p -> p.resolveFor(placeholder.getComponent(), placeholder.getEnvironment()))
                .map(value -> property(placeholder.getComponent(), value, PROPERTIES,
                        new DeclaringComponentImpl(placeholder.getConfigType(), placeholder.getComponent(), placeholder.getEnvironment())));
    }
}