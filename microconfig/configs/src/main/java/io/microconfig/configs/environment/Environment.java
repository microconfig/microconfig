package io.microconfig.configs.environment;

import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static deployment.util.CollectionUtils.singleValue;
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

    public Environment(String name, List<ComponentGroup> componentGroups,
                       Optional<String> ip, Optional<Integer> portOffset, Optional<EnvInclude> include) {
        this.name = requireNonNull(name);
        this.componentGroups = unmodifiableList(requireNonNull(componentGroups));
        this.ip = requireNonNull(ip);
        this.portOffset = requireNonNull(portOffset);
        this.include = requireNonNull(include);
    }

    public List<ComponentGroup> getComponentsGroupByIp(String serverIp) {
        return componentGroups.stream()
                .filter(g -> of(serverIp).equals(g.getIp()))
                .collect(toList());
    }

    public ComponentGroup getComponentGroupByName(String groupName) {
        List<ComponentGroup> collect = componentGroups.stream()
                .filter(g -> g.getName().equals(groupName))
                .collect(toList());

        if (collect.isEmpty()) {
            throw new IllegalArgumentException("Can't find group with name [" + groupName + "] in env [" + name + "]");
        }

        return singleValue(collect);
    }

    public Optional<ComponentGroup> getComponentGroupByComponentName(String componentName) {
        List<ComponentGroup> groups = componentGroups.stream()
                .filter(g -> g.getComponents().stream().anyMatch(c -> c.getName().equals(componentName)))
                .collect(toList());

        return groups.isEmpty() ? empty() : of(singleValue(groups));
    }

    public Optional<Component> getComponentByName(String componentName) {
        return componentGroups.stream()
                .map(componentGroup -> componentGroup.getComponentByName(componentName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public void verifyComponents() {
        Set<String> components = new HashSet<>();
        componentGroups.stream()
                .flatMap(cg -> cg.getComponents().stream())
                .filter(c -> !components.add(c.getName()))
                .findFirst()
                .ifPresent(c -> {
                    throw new IllegalArgumentException(
                            "Env [" + name + "] containsInnerFile several definitions of [" + c.getName() + "] component"
                    );
                });
    }

    public void verifyIpsSet() {
        componentGroups.stream()
                .filter(g -> !ip.isPresent() && !g.getIp().isPresent())
                .findFirst()
                .ifPresent(g -> {
                    throw new IllegalArgumentException("Env [" + name + "] does not have ip for [" + g.getName() + "] componentGroup");
                });
    }

    public Environment processInclude(EnvironmentProvider environmentProvider) {
        if (!include.isPresent()) return this;

        return include.get().includeTo(this, environmentProvider);
    }

    @Override
    public String toString() {
        return name;
    }
}