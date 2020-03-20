package io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.EnvProperty;

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