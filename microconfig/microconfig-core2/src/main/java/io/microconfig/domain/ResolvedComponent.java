package io.microconfig.domain;

import java.util.Map;

public interface ResolvedComponent {
    String getComponentName();

    String getConfigType();

    Map<String, Property> getPropertyByKey();

    <T> T serialize(PropertySerializer<T> serializer);
}