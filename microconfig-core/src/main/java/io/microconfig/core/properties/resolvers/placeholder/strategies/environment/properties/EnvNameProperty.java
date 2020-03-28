package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class EnvNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "env";
    }

    @Override
    public Optional<String> resolveFor(String __, Environment environment) {
        return of(environment.getName());
    }
}