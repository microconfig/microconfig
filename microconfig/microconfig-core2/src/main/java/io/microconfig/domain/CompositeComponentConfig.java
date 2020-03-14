package io.microconfig.domain;

import java.util.List;
import java.util.Optional;

public interface CompositeComponentConfig {
    List<ComponentConfig> asList();

    CompositeComponentConfig resolveBy(Resolver resolver);

    List<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> List<T> save(PropertySerializer<T> serializer);
}