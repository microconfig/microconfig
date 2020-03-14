package io.microconfig.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface ConfigBuildResult {
    String getConfigType();

    ConfigBuildResult build();

    List<Property> getProperties();

    Optional<Property> getProperty(String key);

    ConfigBuildResult forEachProperty(UnaryOperator<Property> operator);

    <T> T save(PropertySerializer<T> serializer);
}