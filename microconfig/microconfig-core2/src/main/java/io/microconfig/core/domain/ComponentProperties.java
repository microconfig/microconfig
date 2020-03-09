package io.microconfig.core.domain;

public interface ComponentProperties {
    String getConfigType();

    <T> T serialize(PropertiesSerializer<T> serializer);
}