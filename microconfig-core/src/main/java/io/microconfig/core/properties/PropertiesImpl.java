package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.microconfig.utils.StreamUtils.*;
import static java.util.function.Function.identity;

@EqualsAndHashCode
@RequiredArgsConstructor
public class PropertiesImpl implements Properties {
    private final List<TypedProperties> properties;

    public static Properties flat(List<Properties> properties) {
        return new PropertiesImpl(flatMapEach(properties, Properties::asTypedProperties));
    }

    @Override
    public Properties resolveBy(Resolver resolver) {
        return withEachComponent(c -> c.resolveBy(resolver));
    }

    @Override
    public Properties withoutVars() {
        return withEachComponent(TypedProperties::withoutVars);
    }

    @Override
    public Properties withPrefix(String prefix) {
        return withEachComponent(tp -> tp.withPrefix(prefix));
    }

    @Override
    public Map<String, Property> getPropertiesAsMap() {
        return propertyKeyTo(identity());
    }

    @Override
    public Map<String, String> getPropertiesAsKeyValue() {
        return propertyKeyTo(Property::getValue);
    }

    @Override
    public Collection<Property> getProperties() {
        return flatMapEach(properties, TypedProperties::getProperties);
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return findFirstResult(properties, p -> p.getPropertyWithKey(key));
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return forEach(properties, p -> p.save(serializer));
    }

    @Override
    public List<TypedProperties> asTypedProperties() {
        return properties;
    }

    @Override
    public TypedProperties first() {
        return properties.get(0);
    }

    @Override
    public Properties forEachComponent(Consumer<TypedProperties> callback) {
        properties.parallelStream().forEach(callback);
        return this;
    }

    private Properties withEachComponent(UnaryOperator<TypedProperties> applyFunction) {
        return new PropertiesImpl(forEach(properties.parallelStream(), applyFunction));
    }

    private <T> Map<String, T> propertyKeyTo(Function<Property, T> valueGetter) {
        return properties.stream()
                .map(TypedProperties::getProperties)
                .flatMap(Collection::stream)
                .collect(toLinkedMap(Property::getKey, valueGetter));
    }
}