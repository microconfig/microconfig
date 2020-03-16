package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.PlaceholderSource;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.domain.impl.properties.PropertyImpl.tempProperty;
import static io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.PlaceholderSource.COMPONENT_SOURCE;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentResolveStrategy implements PlaceholderResolveStrategy {
    private final Map<String, ComponentProperty> propertyByKey;

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        ComponentProperty componentProperty = propertyByKey.get(placeholder.getValue());

        return ofNullable(componentProperty)
                .flatMap(p -> p.value(placeholder.getComponent(), placeholder.getComponent())) //todo
                .map(value -> tempProperty(placeholder.getComponent(), value, placeholder.getComponent(), new PlaceholderSource(placeholder.getComponent(), COMPONENT_SOURCE)));
    }
}