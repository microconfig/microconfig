package io.microconfig.environments;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Getter
public class ComponentGroup {
    private final String name;
    private final Optional<String> ip;
    private final List<Component> components;
    private final List<Component> excludedComponents;
    private final List<Component> appendedComponents;

    public ComponentGroup(String name, Optional<String> ip,
                          List<Component> components,
                          List<Component> excludedComponents,
                          List<Component> appendedComponents) {
        this.name = requireNonNull(name);
        this.ip = requireNonNull(ip);
        if (!components.isEmpty() && (!excludedComponents.isEmpty() || !appendedComponents.isEmpty())) {
            throw new IllegalArgumentException("if you override component list than 'exclude' and 'append' params must be empty");
        }

        this.excludedComponents = unmodifiableList(requireNonNull(excludedComponents));
        this.appendedComponents = unmodifiableList(requireNonNull(appendedComponents));
        this.components = unmodifiableList(requireNonNull(components));
    }

    public ComponentGroup changeIp(String ip) {
        return new ComponentGroup(name, of(ip), components, excludedComponents, appendedComponents);
    }

    public ComponentGroup changeComponents(List<Component> newComponents) {
        return new ComponentGroup(name, ip, newComponents, excludedComponents, appendedComponents);
    }

    public ComponentGroup excludeComponents(List<Component> newExcludedComponents) {
        List<Component> newComponents = new ArrayList<>(this.components);
        newComponents.removeAll(newExcludedComponents);

        return new ComponentGroup(name, ip, newComponents, emptyList(), appendedComponents);
    }

    public ComponentGroup appendComponents(List<Component> newAppendedComponents) {
        List<Component> newComponents = new ArrayList<>(this.components);
        newComponents.addAll(newAppendedComponents);

        return new ComponentGroup(name, ip, newComponents, excludedComponents, emptyList());
    }

    public Optional<Component> getComponentByName(String name) {
        return components.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public List<String> getComponentNames() {
        return components.stream()
                .map(Component::getName)
                .collect(toList());
    }

    @Override
    public String toString() {
        return name;
    }
}