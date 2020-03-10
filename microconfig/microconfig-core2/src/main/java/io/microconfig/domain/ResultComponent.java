package io.microconfig.domain;

import java.util.Map;

public interface ResultComponent {
    String getComponentName();

    String getConfigType();

    Map<String, Property> getPropertyByKey();

    <T> T save(PropertySerializer<T> serializer);
}