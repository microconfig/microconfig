package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Components;
import io.microconfig.domain.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static io.microconfig.io.StreamUtils.filter;
import static io.microconfig.io.StreamUtils.map;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class EnvironmentImpl implements Environment {
    @Getter
    private final String name;
    private final List<ComponentGroup> componentGroups;

    @Override
    public List<ComponentGroup> findGroupsWithIp(String ip) {
        return filter(componentGroups, g -> g.getIp().filter(ip::equals).isPresent());
    }

    @Override
    public ComponentGroup findGroupWithName(String groupName) {
        return filterGroup(g -> g.getName().equals(groupName), () -> "group name=" + groupName);
    }

    @Override
    public ComponentGroup findGroupWithComponent(String componentName) {
        return filterGroup(g -> g.containsComponent(componentName), () -> "component name=" + componentName);
    }

    @Override
    public Components getAllComponents() {
        return new ComponentsImpl(componentGroups.stream()
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(List::stream)
                .collect(toList())
        );
    }

    @Override
    public Component findComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor) {
        return componentGroups.stream()
                .filter(g -> g.containsComponent(componentName))
                .map(g -> g.getComponentWithName(componentName))
                .findFirst()
                .orElseGet(() -> null);//todo
    }

    @Override
    public Components findComponentsFrom(List<String> groups, List<String> components) {
        Supplier<List<Component>> componentsFromGroups = () -> {
            if (groups.isEmpty()) return getAllComponents().asList();

            return groups.stream()
                    .map(this::findGroupWithName)
                    .flatMap(g -> g.getComponents().asList().stream())
                    .collect(toList());
        };

        UnaryOperator<List<Component>> filterByComponents = (componentFromGroups) -> {
            if (components.isEmpty()) return componentFromGroups;

            Map<String, Component> componentByName = componentFromGroups.stream()
                    .collect(toMap(Component::getName, identity()));
            return map(components,
                    name -> requireNonNull(componentByName.get(name), () -> "Component '" + name + "' is not configured for " + name + " env"));
        };

        List<Component> componentFromGroups = componentsFromGroups.get();
        return new ComponentsImpl(filterByComponents.apply(componentFromGroups));
    }

    private ComponentGroup filterGroup(Predicate<ComponentGroup> predicate, Supplier<String> filter) {
        return componentGroups.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find group by " + filter.get() + " in env [" + name + "]"));
    }
}
