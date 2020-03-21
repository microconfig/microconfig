package io.microconfig.core.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Optional;

import static java.util.Optional.of;

public class GroupNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "group";
    }

    @Override //todo
    public Optional<String> resolveFor(String component, Environment environment) {
        return of(environment.findGroupWithComponent(component).getName());
    }
}
