package io.microconfig.commands;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentProvider;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

public class CommandContext {
    private final String env;
    private final Optional<String> componentGroup;
    private final List<String> components;

    public CommandContext(String env, List<String> components) {
        this(env, empty(), components);
    }

    public CommandContext(String env, Optional<String> componentGroup, List<String> components) {
        this.env = requireNonNull(env, "Env is null");
        this.componentGroup = requireNonNull(componentGroup, "Component group optional is null");
        this.components = requireNonNull(components);
    }

    public List<Component> components(EnvironmentProvider environmentProvider) {
        List<Component> envComponents = componentsByEnvAndGroups(environmentProvider);
        return components.isEmpty() ? envComponents : filterByName(envComponents);
    }

    public String env() {
        return env;
    }

    private List<Component> componentsByEnvAndGroups(EnvironmentProvider environmentProvider) {
        Environment environment = environmentProvider.getByName(env);

        return componentGroup.isPresent() ?
                environment.getComponentsByGroup(componentGroup.get())
                : environment.getAllComponents();
    }

    private List<Component> filterByName(List<Component> envComponents) {
        return components.stream()
                .map(name -> envComponents.stream()
                        .filter(c -> c.getName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Component '" + name + "' is not configured for " + env + " env")))
                .collect(toList());
    }
}