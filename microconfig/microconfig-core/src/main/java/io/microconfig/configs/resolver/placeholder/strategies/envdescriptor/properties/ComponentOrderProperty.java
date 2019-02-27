package io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy.EnvProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;

import java.util.Optional;

public class ComponentOrderProperty implements EnvProperty {
    @Override
    public String key() {
        return "order";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getGroupByComponentName(component.getName())
                .map(cg -> cg.getComponentNames().indexOf(component.getName()))
                .map(String::valueOf);
    }
}