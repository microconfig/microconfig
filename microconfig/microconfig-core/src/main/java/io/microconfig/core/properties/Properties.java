package io.microconfig.core.properties;

import java.util.List;

public interface Properties {
    Properties resolveBy(Resolver resolver);

    Properties withoutTempValues();

    <T> List<T> save(PropertySerializer<T> serializer);

    List<TypedProperties> asList();
}