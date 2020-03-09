package io.microconfig.core.domain;

import java.util.Map;

public interface ComponentProperties {
    Map<String, Property> getPropertyByKey();

    String getConfigType();

    <T> T serialize(PropertiesSerializer<T> serializer);
}