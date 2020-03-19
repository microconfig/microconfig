package io.microconfig.core.properties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ComponentProperties {
    String getConfigType();

    ComponentProperties resolveBy(StatementResolver resolver);

    ComponentProperties withoutTempValues();

    List<Property> getProperties();

    Map<String, String> propertiesAsKeyValue();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}