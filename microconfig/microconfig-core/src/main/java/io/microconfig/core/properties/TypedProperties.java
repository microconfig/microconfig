package io.microconfig.core.properties;

import java.util.Map;
import java.util.Optional;

public interface TypedProperties {
    String getComponent();

    String getConfigType();

    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutTempValues();

    Map<String, Property> getProperties();

    Map<String, String> propertiesAsKeyValue();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}