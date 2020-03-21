package io.microconfig.core.resolvers.placeholder.strategies.environment.properties;


import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class ComponentOrderProperty implements EnvProperty {
    @Override
    public String key() {
        return "order";
    }

    @Override //todo
    public Optional<String> resolveFor(String component, Environment environment) {
        List<Component> components = environment.findGroupWithComponent(component)
                .getComponents()
                .asList();

        return IntStream.range(0, components.size())
                .filter(i -> components.get(i).getName().equals(component))
                .mapToObj(String::valueOf)
                .findFirst();
    }
}