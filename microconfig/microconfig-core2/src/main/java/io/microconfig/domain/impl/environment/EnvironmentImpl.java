package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Components;
import io.microconfig.domain.Environment;
import io.microconfig.io.StreamUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static io.microconfig.io.StreamUtils.filter;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class EnvironmentImpl implements Environment {
    @Getter
    private final String name;
    private final List<ComponentGroup> componentGroups;
    private final ComponentFactory componentFactory;

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
        Optional<Component> component = componentGroups.stream()
                .filter(g -> g.containsComponent(componentName))
                .map(g -> g.getComponentWithName(componentName))
                .findFirst();

        if (component.isPresent()) return component.get();
        if (!mustBeDeclaredInEnvDescriptor) return createComponentWithName(componentName);
        throw new IllegalArgumentException(exceptionMessageForComponent(componentName));
    }

    private Component createComponentWithName(String componentName) {
        return componentFactory.createComponent(componentName, componentName, name);
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
                    .collect(toMap(Component::getAlias, identity()));
            return StreamUtils.forEach(components, name -> requireNonNull(componentByName.get(name), () -> exceptionMessageForComponent(name)));
        };

        List<Component> componentFromGroups = componentsFromGroups.get();
        return new ComponentsImpl(filterByComponents.apply(componentFromGroups));
    }

    private String exceptionMessageForComponent(String component) {
        return "Component '" + component + "' is not configured for env '" + name + "'";
    }

    private ComponentGroup filterGroup(Predicate<ComponentGroup> predicate, Supplier<String> filter) {
        return componentGroups.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find group by " + filter.get() + " in env [" + name + "]"));
    }
}
