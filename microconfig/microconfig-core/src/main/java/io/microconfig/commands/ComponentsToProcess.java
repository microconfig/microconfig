package io.microconfig.commands;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentsToProcess {
    private final String env;
    private final String componentGroup;
    private final List<String> components;

    public ComponentsToProcess(String env, List<String> components) {
        this(env, null, components);
    }

    public List<Component> components(EnvironmentProvider environmentProvider) {
        ComponentGroup group = getGroup(environmentProvider);
        return filterByComponents(group);
    }

    public String env() {
        return env;
    }

    private ComponentGroup getGroup(EnvironmentProvider environmentProvider) {
        Environment environment = environmentProvider.getByName(env);
        return componentGroup == null ? ComponentGroup.of(environment.getAllComponents()) : environment.getGroupByName(componentGroup);
    }

    private List<Component> filterByComponents(ComponentGroup group) {
        if (components.isEmpty()) return group.getComponents();

        return components.stream()
                .map(name -> getComponentByName(name, group))
                .collect(toList());
    }

    private Component getComponentByName(String name, ComponentGroup group) {
        return group.getComponentByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Component '" + name + "' is not configured for " + env + " env"));
    }
}