package io.microconfig.core.environments;

import lombok.Getter;
import lombok.With;

import java.util.*;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static io.microconfig.utils.StreamUtils.findFirst;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Getter
public class Environment {
    private final String name;
    private final List<ComponentGroup> componentGroups;

    private final Optional<String> ip;
    private final Optional<Integer> portOffset;
    private final Optional<EnvInclude> include;

    @With
    private final Object source;

    public Environment(String name,
                       List<ComponentGroup> componentGroups,
                       Optional<String> ip, Optional<Integer> portOffset,
                       Optional<EnvInclude> include,
                       Object source) {
        this.name = requireNonNull(name);
        this.componentGroups = unmodifiableList(requireNonNull(componentGroups));
        this.ip = requireNonNull(ip);
        this.portOffset = requireNonNull(portOffset);
        this.include = requireNonNull(include);
        this.source = source;
    }

    public List<ComponentGroup> getGroupByIp(String serverIp) {
        return componentGroups.stream()
                .filter(g -> g.getIp().filter(serverIp::equals).isPresent())
                .collect(toList());
    }

    public ComponentGroup getGroupByName(String groupName) {
        List<ComponentGroup> groups = componentGroups.stream()
                .filter(g -> g.getName().equals(groupName))
                .collect(toList());

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Can't find group with name [" + groupName + "] in env [" + name + "]");
        }

        return singleValue(groups);
    }

    public Optional<ComponentGroup> getGroupByComponentName(String componentName) {
        List<ComponentGroup> groups = componentGroups.stream()
                .filter(g -> g.getComponents().stream().anyMatch(c -> c.getName().equals(componentName)))
                .collect(toList());

        return groups.isEmpty() ? empty() : of(singleValue(groups));
    }

    public List<Component> getComponentsByGroup(String group) {
        return getGroupByName(group).getComponents();
    }

    public List<Component> getAllComponents() {
        return getComponentGroups()
                .stream()
                .flatMap(group -> group.getComponents().stream())
                .collect(toList());
    }

    public Optional<Component> getComponentByName(String componentName) {
        return findFirst(componentGroups, componentGroup -> componentGroup.getComponentByName(componentName));
    }

    public Environment withIncludedGroups(List<ComponentGroup> includedGroups) {
        return new Environment(
                name,
                includedGroups,
                ip,
                portOffset,
                empty(),
                source
        );
    }

    public Environment verifyUniqueComponentNames() {
        Set<String> unique = new HashSet<>();
        componentGroups.stream()
                .map(ComponentGroup::getComponents)
                .flatMap(Collection::stream)
                .filter(c -> !unique.add(c.getName()))
                .findFirst()
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Env [" + name + "] contains several definitions of [" + c.getName() + "] component");
                });

        return this;
    }

    public Environment processInclude(EnvironmentProvider environmentProvider) {
        return include.map(env -> env.includeTo(this, environmentProvider))
                .orElse(this);
    }

    @Override
    public String toString() {
        return name;
    }
}