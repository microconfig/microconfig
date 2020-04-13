package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import static io.microconfig.utils.StreamUtils.*;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static lombok.AccessLevel.PRIVATE;

@EqualsAndHashCode
@RequiredArgsConstructor
public class TypedPropertiesImpl implements TypedProperties {
    @Getter
    private final ConfigType configType;
    private final String component;
    private final String environment;
    @With(PRIVATE)
    private final Map<String, Property> propertyByKey;

    @Override
    public DeclaringComponent getDeclaringComponent() {
        return new DeclaringComponentImpl(configType.getName(), component, environment);
    }

    @Override
    public TypedProperties resolveBy(Resolver resolver) {
        return withPropertyByKey(
                forEach(propertyByKey.values(), resolveUsing(resolver), toPropertyMap())
        );
    }

    @Override
    public TypedProperties withoutTempValues() {
        return withPropertyByKey(
                filter(propertyByKey.values(), p -> !p.isTemp(), toPropertyMap())
        );
    }

    @Override
    public TypedProperties withPrefix(String prefix) {
        return withPropertyByKey(
            filter(propertyByKey.values(), p -> p.getKey().startsWith(prefix), toPropertyMap())
        );
    }

    @Override
    public Map<String, Property> getPropertiesAsMap() {
        return propertyByKey;
    }

    @Override
    public Map<String, String> getPropertiesAsKeyValue() {
        return propertyByKey.values()
                .stream()
                .collect(toLinkedMap(Property::getKey, Property::getValue));
    }

    @Override
    public Collection<Property> getProperties() {
        return propertyByKey.values();
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return ofNullable(propertyByKey.get(key));
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(propertyByKey.values(), configType, component, environment);
    }

    @Override
    public String toString() {
        return getDeclaringComponent().toString();
    }

    private UnaryOperator<Property> resolveUsing(Resolver resolver) {
        DeclaringComponent root = getDeclaringComponent();
        return property -> property.resolveBy(resolver, root);
    }

    private Collector<Property, ?, Map<String, Property>> toPropertyMap() {
        return toLinkedMap(Property::getKey, identity());
    }
}