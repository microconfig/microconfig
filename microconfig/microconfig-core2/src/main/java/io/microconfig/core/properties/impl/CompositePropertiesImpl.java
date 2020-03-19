package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.microconfig.core.properties.impl.PropertyImpl.asKeyValue;
import static io.microconfig.utils.StreamUtils.*;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class CompositePropertiesImpl implements CompositeProperties {
    private final List<Properties> properties;

    public static CompositeProperties composite(List<Properties> properties) {
        return new CompositePropertiesImpl(properties);
    }

    @Override
    public List<Properties> asList() {
        return properties;
    }

    @Override
    public CompositeProperties withoutTempValues() {
        return forEachComponent(Properties::withoutTempValues);
    }

    @Override
    public CompositeProperties resolveBy(Resolver resolver) {
        return forEachComponent(c -> c.resolveBy(resolver));
    }

    @Override
    public List<Property> getProperties() {
        return flatMapEach(properties, Properties::getProperties);
    }

    @Override
    public Map<String, String> propertiesAsKeyValue() {
        return asKeyValue(getProperties());
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return firstFirstResult(properties, r -> r.getPropertyWithKey(key));
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return forEach(properties, p -> p.save(serializer));
    }

    private CompositeProperties forEachComponent(UnaryOperator<Properties> func) {
        return composite(forEach(properties, func));
    }
}