package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Components;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.IntStream.range;

public class ComponentOrderProperty implements EnvProperty {
    @Override
    public String key() {
        return "order";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
        return environment.findGroupWithComponent(component)
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(findIndexOf(component));
    }

    private Function<List<Component>, Optional<String>> findIndexOf(String component) {
        return components -> range(0, components.size())
                .filter(i -> components.get(i).getName().equals(component))
                .mapToObj(String::valueOf)
                .findFirst();
    }
}