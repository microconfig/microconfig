package io.microconfig.core.properties;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface TypedProperties {
    String getComponent();

    String getConfigType();

    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutTempValues();

    Map<String, Property> getPropertyByKeyMap();

    Map<String, String> getPropertyValueByKeyMap();

    Collection<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}