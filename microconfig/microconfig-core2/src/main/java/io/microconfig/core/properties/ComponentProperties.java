package io.microconfig.core.properties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ComponentProperties {
    String getComponent();

    String getConfigType();

    ComponentProperties resolveBy(Resolver resolver);

    ComponentProperties withoutTempValues();

    List<Property> getProperties();

    Map<String, String> propertiesAsKeyValue();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}