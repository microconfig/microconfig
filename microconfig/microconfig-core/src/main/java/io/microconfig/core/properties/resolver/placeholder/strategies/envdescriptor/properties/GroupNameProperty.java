package io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class GroupNameProperty implements EnvProperty {
    @Override
    public String key() {
        return "group";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getGroupByComponentName(component.getName())
                .map(ComponentGroup::getName);
    }
}
