package io.microconfig.core.properties;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface TypedProperties {
    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutTempValues();

    Map<String, Property> getPropertiesAsMap();

    Map<String, String> getPropertiesAsKeyValue();

    Collection<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);

    DeclaringComponent getDeclaringComponent();
}