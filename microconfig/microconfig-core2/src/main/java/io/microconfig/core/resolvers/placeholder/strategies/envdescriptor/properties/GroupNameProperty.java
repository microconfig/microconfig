package io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class GroupNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "group";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
//        return environment.findGroupWithComponent(componentName).getName();
        return Optional.empty();
    }
}
