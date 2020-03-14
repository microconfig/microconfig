package io.microconfig.domain;

import java.util.List;
import java.util.Optional;

public interface ComponentConfiguration {
    String getConfigType();

    ComponentConfiguration resolveBy(Resolver resolver);

    List<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}