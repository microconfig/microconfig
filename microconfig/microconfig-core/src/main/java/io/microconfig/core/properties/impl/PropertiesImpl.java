package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertySerializer;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.UnaryOperator;

import static io.microconfig.utils.StreamUtils.flatMapEach;
import static io.microconfig.utils.StreamUtils.forEach;

@EqualsAndHashCode
@RequiredArgsConstructor
public class PropertiesImpl implements Properties {
    private final List<TypedProperties> properties;

    public static Properties composite(List<Properties> properties) {
        return new PropertiesImpl(flatMapEach(properties, Properties::asList));
    }

    @Override
    public List<TypedProperties> asList() {
        return properties;
    }

    @Override
    public Properties withoutTempValues() {
        return forEachComponent(TypedProperties::withoutTempValues);
    }

    @Override
    public Properties resolveBy(Resolver resolver) {
        return forEachComponent(c -> c.resolveBy(resolver));
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return forEach(properties, p -> p.save(serializer));
    }

    private Properties forEachComponent(UnaryOperator<TypedProperties> applyFunction) {
        return new PropertiesImpl(forEach(properties, applyFunction));
    }
}