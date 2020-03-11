package io.microconfig.domain;

import java.util.List;

public interface ConfigBuildResults {
    List<ConfigBuildResult> asList();

    ConfigBuildResult first();

    <T> List<T> save(PropertySerializer<T> serializer);
}