package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.io.yaml.YamlTreeImpl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.StreamUtils.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
    public TypedProperties withoutVars() {
        return filterProperties(p -> !p.isVar());
    }

    @Override
    public TypedProperties withPrefix(String prefix) {
        return filterProperties(p -> p.getKey().startsWith(prefix));
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
        Property property = propertyByKey.get(key);
        return property != null ? of(property) : tryFindByPrefix(key);
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

    private TypedProperties filterProperties(Predicate<Property> filter) {
        return withPropertyByKey(filter(propertyByKey.values(), filter, toPropertyMap()));
    }

    private Collector<Property, ?, Map<String, Property>> toPropertyMap() {
        return toLinkedMap(Property::getKey, identity());
    }

    private Optional<Property> tryFindByPrefix(String originalKey) {
        if (!originalKey.endsWith(".*")) return empty();

        String key = originalKey.substring(0, originalKey.length() - 2);
        Collection<Property> withPrefix = withPrefix(key).getProperties();
        if (withPrefix.isEmpty()) return empty();

        return of(property(key, toYaml(withPrefix, key), YAML, getDeclaringComponent()));
    }

    private String toYaml(Collection<Property> withPrefix, String key) {
        Map<String, String> yaml = withPrefix.stream()
                .collect(toLinkedMap(property -> property.getKey().substring(key.length() + 1), Property::getValue));
        return new YamlTreeImpl(false).toYaml(yaml);
    }
}