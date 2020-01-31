package io.microconfig.commands;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class CommandContext {
    private final String env;
    private final String componentGroup;
    private final List<String> components;

    public CommandContext(String env, List<String> components) {
        this(env, null, components);
    }

    public String env() {
        return env;
    }

    public List<Component> components(EnvironmentProvider environmentProvider) {
        List<Component> envComponents = componentsByEnvAndGroups(environmentProvider);
        return components.isEmpty() ? envComponents : filterByName(envComponents);
    }

    private List<Component> componentsByEnvAndGroups(EnvironmentProvider environmentProvider) {
        Environment environment = environmentProvider.getByName(env);

        return componentGroup != null ?
                environment.getComponentsByGroup(componentGroup) :
                environment.getAllComponents();
    }

    private List<Component> filterByName(List<Component> envComponents) {
        return components.stream()
                .map(name -> findByName(name, envComponents))
                .collect(toList());
    }

    private Component findByName(String name, List<Component> envComponents) {
        return envComponents.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '" + name + "' is not configured for " + env + " env"));
    }
}