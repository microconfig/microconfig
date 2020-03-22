package io.microconfig.core.properties;

import java.util.List;
import java.util.Optional;

public interface Properties {
    Properties resolveBy(Resolver resolver);

    Properties withoutTempValues();

    Optional<Property> getPropertyWithKey(String key);

    <T> List<T> save(PropertySerializer<T> serializer);

    List<TypedProperties> asList();
}