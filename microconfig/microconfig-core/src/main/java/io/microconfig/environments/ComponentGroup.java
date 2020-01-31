package io.microconfig.environments;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.CollectionUtils.join;
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
            throw new IllegalArgumentException("if you override component list than 'exclude' and 'append' params must be empty.");
        }

        this.excludedComponents = unmodifiableList(requireNonNull(excludedComponents));
        this.appendedComponents = unmodifiableList(requireNonNull(appendedComponents));
        this.components = unmodifiableList(requireNonNull(components));
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

    public ComponentGroup changeIp(String ip) {
        return new ComponentGroup(name, of(ip), components, excludedComponents, appendedComponents);
    }

    public ComponentGroup override(ComponentGroup override) {
        ComponentGroup result = this;

        if (override.ip.isPresent()) {
            result = result.changeIp(override.ip.get());
        }
        if (!override.components.isEmpty()) {
            result = result.changeComponents(override.components);
        }
        if (!override.excludedComponents.isEmpty()) {
            result = result.excludeComponents(override.excludedComponents);
        }
        if (!override.appendedComponents.isEmpty()) {
            result = result.appendComponents(override.appendedComponents);
        }

        return result;
    }

    private ComponentGroup changeComponents(List<Component> newComponents) {
        return new ComponentGroup(name, ip, newComponents, excludedComponents, appendedComponents);
    }

    private ComponentGroup excludeComponents(List<Component> toExclude) {
        List<Component> withoutExcluded = new ArrayList<>(components);
        withoutExcluded.removeAll(toExclude);

        return new ComponentGroup(name, ip, withoutExcluded, emptyList(), appendedComponents);
    }

    private ComponentGroup appendComponents(List<Component> newAppendedComponents) {
        return new ComponentGroup(name, ip, join(this.components, newAppendedComponents), excludedComponents, emptyList());
    }

    @Override
    public String toString() {
        return name;
    }
}