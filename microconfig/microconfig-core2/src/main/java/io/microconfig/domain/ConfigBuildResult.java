package io.microconfig.domain;

import java.util.Map;

public interface ConfigBuildResult {
    String getComponentName();

    String getConfigType();

    Map<String, Property> getPropertyByKey();

    <T> T save(PropertySerializer<T> serializer);
}