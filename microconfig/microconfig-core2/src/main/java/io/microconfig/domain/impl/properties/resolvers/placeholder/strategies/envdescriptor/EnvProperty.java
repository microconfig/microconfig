package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor;

import io.microconfig.domain.Environment;

import java.util.Optional;

public interface EnvProperty {
    String key();

    Optional<String> value(String componentName, String componentType, Environment environment);
}
