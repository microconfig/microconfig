package io.microconfig.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ComponentProperties {
    String getConfigType();

    ComponentProperties withoutTempValues();

    ComponentProperties resolveBy(StatementResolver resolver);

    List<Property> getProperties();

    Map<String, String> propertiesAsKeyValue();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}