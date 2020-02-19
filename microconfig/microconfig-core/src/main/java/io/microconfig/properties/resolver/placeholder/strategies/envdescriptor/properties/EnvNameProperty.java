package io.microconfig.properties.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.envdescriptor.EnvProperty;

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
