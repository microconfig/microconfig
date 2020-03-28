package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Optional;

public class GroupNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "group";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
        return environment.findGroupWithComponent(component)
                .map(ComponentGroup::getName);
    }
}