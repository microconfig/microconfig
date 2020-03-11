package io.microconfig.domain;

import java.util.List;
import java.util.function.UnaryOperator;

public interface ConfigBuildResult {
    String getConfigType();

    List<Property> getProperties();

    ConfigBuildResult forEachProperty(UnaryOperator<Property> operator);

    <T> T save(PropertySerializer<T> serializer);
}