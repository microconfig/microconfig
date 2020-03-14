package io.microconfig.domain;

import java.util.List;
import java.util.Optional;

public interface ComponentConfig {
    String getConfigType();

    ComponentConfig resolveBy(Resolver resolver);

    List<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}