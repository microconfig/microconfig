package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor.properties;

import io.microconfig.domain.Environment;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class EnvNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "env";
    }

    @Override
    public Optional<String> value(String componentName, String componentType, Environment environment) {
        return of(environment.getName());
    }
}