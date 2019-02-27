package io.microconfig.configs.resolver.placeholder.strategies;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.placeholder.ResolveStrategy;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.configs.Property.tempProperty;
import static io.microconfig.configs.PropertySource.specialSource;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class GeneralPropertiesResolveStrategy implements ResolveStrategy {
    private final Map<String, GeneralProperty> properties;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String envName) {
        GeneralProperty specialProperty = properties.get(propertyKey);

        return ofNullable(specialProperty)
                .flatMap(p -> p.value(component))
                .map(value -> tempProperty(propertyKey, value, envName, specialSource(component, "generalProperties")));
    }

    public interface GeneralProperty {
        String key();

        Optional<String> value(Component component);
    }
}