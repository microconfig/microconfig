package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface TypedProperties {
    ConfigType getConfigType();

    DeclaringComponent getDeclaringComponent();

    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutTempValues();

    TypedProperties withPrefix(String prefix);

    Map<String, Property> getPropertiesAsMap();

    Map<String, String> getPropertiesAsKeyValue();

    Collection<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}