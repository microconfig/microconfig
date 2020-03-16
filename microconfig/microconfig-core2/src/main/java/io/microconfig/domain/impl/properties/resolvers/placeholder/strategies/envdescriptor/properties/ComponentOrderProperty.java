package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor.properties;


import io.microconfig.domain.Environment;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class ComponentOrderProperty implements EnvProperty {
    @Override
    public String key() {
        return "order";
    }

    @Override
    public Optional<String> value(String componentName, String componentType, Environment environment) {
        return environment.findGroupWithComponent(componentName)
                .map(cg -> cg().indexOf(componentName))
                .map(String::valueOf);
    }
}