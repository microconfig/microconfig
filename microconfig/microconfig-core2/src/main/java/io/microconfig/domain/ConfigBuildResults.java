package io.microconfig.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface ConfigBuildResults {
    List<ConfigBuildResult> asList();

    ConfigBuildResults build();

    List<Property> getProperties();

    Optional<Property> getProperty(String key);

    ConfigBuildResults forEachProperty(UnaryOperator<Property> operator);

    <T> List<T> save(PropertySerializer<T> serializer);
}