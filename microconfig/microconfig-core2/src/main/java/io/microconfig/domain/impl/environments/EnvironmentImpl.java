package io.microconfig.domain.impl.environments;

import io.microconfig.domain.*;
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
        return componentFactory.toComponents(
                componentGroups.stream()
                        .map(ComponentGroup::getComponents)
                        .map(Components::asList)
                        .flatMap(List::stream)
                        .collect(toList())
        );
    }

    @Override
    public Component findComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor) {
        return firstFirstResult(componentGroups, g -> g.findComponentWithName(componentName))
                .orElseGet(() -> {
                    if (mustBeDeclaredInEnvDescriptor) {
                        throw new IllegalArgumentException(notFoundComponentMessage(componentName));
                    }
                    return createComponentWithName(componentName);
                });
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
        return componentFactory.toComponents(filterByComponents.apply(componentFromGroups));
    }

    private String notFoundComponentMessage(String component) {
        return "Component '" + component + "' is not configured for env '" + name + "'";
    }

    private ComponentGroup findGroup(Predicate<ComponentGroup> groupPredicate, Supplier<String> description) {
        return componentGroups.stream()
                .filter(groupPredicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find group by filter: '" + description.get() + "' in env '" + name + "'"));
    }

    @Override
    public String toString() {
        return name + ": " + componentGroups;
    }
}
