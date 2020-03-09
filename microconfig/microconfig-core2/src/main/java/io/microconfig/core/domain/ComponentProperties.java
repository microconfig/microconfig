package io.microconfig.core.domain;

import java.util.Map;

public interface ComponentProperties {
    String getConfigType();

    Map<String, Property> getPropertyByKey();

    <T> T serialize(PropertiesSerializer<T> serializer);
}