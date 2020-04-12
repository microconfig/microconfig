package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class PortOffsetProperty implements EnvProperty {
    @Override
    public String key() {
        return "portOffset";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
        return of(environment.getPortOffset())
                .map(String::valueOf);
    }
}