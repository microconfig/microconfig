package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.microconfig.core.properties.impl.PropertyImpl.asKeyValue;
import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.forEach;
import static lombok.AccessLevel.PRIVATE;

@EqualsAndHashCode
@RequiredArgsConstructor
public class TypedPropertiesImpl implements TypedProperties {
    private final ConfigType configType;
    @Getter
    private final String component;
    private final String environment;
    @Getter
    @With(PRIVATE)
    private final List<Property> properties;

    @Override
    public String getConfigType() {
        return configType.getName();
    }

    @Override
    public TypedProperties withoutTempValues() {
        return withProperties(filter(properties, p -> !p.isTemp()));
    }

    @Override
    public TypedProperties resolveBy(Resolver resolver) {
        return withProperties(forEach(properties, resolvePropertyBy(resolver)));
    }

    @Override
    public Map<String, String> propertiesAsKeyValue() {
        return asKeyValue(getProperties());
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return properties.stream()
                .filter(p -> p.getKey().equals(key))
                .findFirst();
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(properties, configType, component, environment);
    }

    @Override
    public String toString() {
        return currentComponent().toString();
    }

    private UnaryOperator<Property> resolvePropertyBy(Resolver resolver) {
        ComponentWithEnv current = currentComponent();
        return p -> p.resolveBy(resolver, current);
    }

    private ComponentWithEnv currentComponent() {
        return new ComponentWithEnv(configType.getName(), component, environment);
    }
}