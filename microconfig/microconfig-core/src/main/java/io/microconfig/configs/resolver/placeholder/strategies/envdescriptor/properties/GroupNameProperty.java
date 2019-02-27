package io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy.EnvProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.Environment;

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
