package io.microconfig.core.properties;

import java.util.Map;

public interface TypedProperties {
    String getComponent();

    String getConfigType();

    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutTempValues();

    Map<String, Property> getProperties();

    <T> T save(PropertySerializer<T> serializer);
}