package io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class EnvNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "env";
    }

    @Override
    public Optional<String> value(Component ignore, Environment environment) {
        return of(environment.getName());
    }
}
