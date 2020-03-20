package io.microconfig.core.resolvers.placeholder.strategies.environment.properties;


import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Optional;

public class ComponentOrderProperty implements EnvProperty {
    @Override
    public String key() {
        return "order";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
//        return environment.findGroupWithComponent(componentName)
//                .map(cg -> cg().indexOf(componentName))
//                .map(String::valueOf);
        return Optional.empty();
    }
}