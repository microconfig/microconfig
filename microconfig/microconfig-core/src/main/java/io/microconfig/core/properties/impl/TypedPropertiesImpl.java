package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Map;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.function.Function.identity;
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
    private final Map<String, Property> properties;

    @Override
    public String getConfigType() {
        return configType.getName();
    }

    @Override
    public TypedProperties withoutTempValues() {
//        return withProperties(filter(properties, p -> !p.isTemp()));
        return this;
    }

    @Override
    public TypedProperties resolveBy(Resolver resolver) {
        ComponentWithEnv root = currentComponent();
        Map<String, Property> resolved = properties.values()
                .stream()
                .map(p -> p.resolveBy(resolver, root))
                .collect(toLinkedMap(Property::getKey, identity()));
        return withProperties(resolved);
    }


    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(properties.values(), configType, component, environment);
    }

    @Override
    public String toString() {
        return currentComponent().toString();
    }

    private ComponentWithEnv currentComponent() {
        return new ComponentWithEnv(configType.getName(), component, environment);
    }
}