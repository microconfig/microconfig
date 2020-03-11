package io.microconfig.domain;

import java.util.Map;
import java.util.function.UnaryOperator;

public interface ConfigBuildResult {
    String getConfigType();

    Map<String, Property> getPropertyByKey();

    ConfigBuildResult applyForEachProperty(UnaryOperator<Property> operator);

    <T> T save(PropertySerializer<T> serializer);
}