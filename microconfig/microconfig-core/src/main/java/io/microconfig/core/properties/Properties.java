package io.microconfig.core.properties;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Properties {
    Properties resolveBy(Resolver resolver);

    Properties withoutTempValues();

    Collection<Property> getProperties();

    Object propertiesAsKeyValue();

    Optional<Property> getPropertyWithKey(String key);

    <T> List<T> save(PropertySerializer<T> serializer);

    List<TypedProperties> asTypedProperties();
}