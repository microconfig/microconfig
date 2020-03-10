package io.microconfig.domain;

import java.util.Map;

public interface ResolvedProperties {
    Map<String, Property> getPropertyByKey();

    String getConfigType();

    <T> T serialize(PropertiesSerializer<T> serializer);
}