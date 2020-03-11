package io.microconfig.domain;

import java.util.List;
import java.util.function.UnaryOperator;

public interface ConfigBuildResults {
    List<ConfigBuildResult> asList();

    ConfigBuildResult first();

    ConfigBuildResults applyForEachProperty(UnaryOperator<Property> operator);

    <T> List<T> save(PropertySerializer<T> serializer);
}