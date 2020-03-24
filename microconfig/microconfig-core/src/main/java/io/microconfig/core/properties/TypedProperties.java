package io.microconfig.core.properties;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface TypedProperties {
    DeclaringComponent getDeclaringComponent();

    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutTempValues();

    Map<String, Property> getPropertiesAsMap();

    Map<String, String> propertiesAsKeyValue();

    Collection<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}