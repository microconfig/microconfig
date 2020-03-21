package io.microconfig.core.environments.impl;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Components;
import io.microconfig.core.environments.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static io.microconfig.utils.StreamUtils.*;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class EnvironmentImpl implements Environment {
    @Getter
    private final String name;
    @Getter
    private final int portOffset;
    private final List<ComponentGroup> componentGroups;
    private final ComponentFactory componentFactory;

    @Override
    public List<ComponentGroup> findGroupsWithIp(String ip) {
        return filter(componentGroups, g -> g.getIp().filter(ip::equals).isPresent());
    }

    @Override
    public ComponentGroup findGroupWithName(String groupName) {
        return findGroup(group -> group.getName().equals(groupName),
                () -> "groupName=" + groupName);
    }

    @Override
    public ComponentGroup findGroupWithComponent(String componentName) {
        return findGroup(group -> group.findComponentWithName(componentName).isPresent(),
                () -> "componentName=" + componentName);
    }

    @Override
    public Components getAllComponents() {
        List<Component> components = componentGroups.stream()
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(List::stream)
                .collect(toList());

        return new ComponentsImpl(components);
    }

    @Override
    //todo must work 0(1)
    //todo maybe just make 2 private methods public instead of this flag check?
    public Component findComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor) {
        return mustBeDeclaredInEnvDescriptor
                ? getComponent(componentName)
                : findOrCreateComponent(componentName);
    }

    private Component getComponent(String componentName) {
        return findFirstResult(componentGroups, g -> g.findComponentWithName(componentName))
                .orElseThrow(() -> new IllegalArgumentException(notFoundComponentMessage(componentName)));
    }

    private Component findOrCreateComponent(String componentName) {
        return findFirstResult(componentGroups, g -> g.findComponentWithName(componentName))
                .orElseGet(() -> componentFactory.createComponent(componentName, componentName, name));
    }

    @Override
    public Components findComponentsFrom(List<String> groups, List<String> components) {
        Supplier<List<Component>> componentsFromGroups = () -> {
            if (groups.isEmpty()) return getAllComponents().asList();

            return groups.stream()
                    .map(this::findGroupWithName)
                    .map(ComponentGroup::getComponents)
                    .map(Components::asList)
                    .flatMap(List::stream)
                    .collect(toList());
        };

        UnaryOperator<List<Component>> filterByComponents = componentFromGroups -> {
            if (components.isEmpty()) return componentFromGroups;

            Map<String, Component> componentByName = componentFromGroups.stream()
                    .collect(toMap(Component::getName, identity()));
            return forEach(components, name -> requireNonNull(componentByName.get(name), () -> notFoundComponentMessage(name)));
        };

        List<Component> componentFromGroups = componentsFromGroups.get();
        return new ComponentsImpl(filterByComponents.apply(componentFromGroups));
    }

    private ComponentGroup findGroup(Predicate<ComponentGroup> groupPredicate, Supplier<String> description) {
        return componentGroups.stream()
                .filter(groupPredicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find group by filter: '" + description.get() + "' in env '" + name + "'"));
    }

    private String notFoundComponentMessage(String component) {
        return "Component '" + component + "' is not configured for env '" + name + "'";
    }

    @Override
    public String toString() {
        return name + ": " + componentGroups;
    }
}
