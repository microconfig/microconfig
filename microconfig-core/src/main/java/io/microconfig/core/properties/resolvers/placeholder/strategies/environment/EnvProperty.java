package io.microconfig.core.properties.resolvers.placeholder.strategies.environment;

import io.microconfig.core.environments.Environment;

import java.util.Optional;

public interface EnvProperty {
    String key();

    Optional<String> resolveFor(String component, Environment environment);
}
