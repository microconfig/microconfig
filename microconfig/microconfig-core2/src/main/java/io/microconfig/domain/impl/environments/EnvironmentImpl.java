package io.microconfig.domain.impl.environments;

import io.microconfig.domain.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.microconfig.domain.impl.properties.ComponentsCollector.toComponents;
import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.firstFirstResult;

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
        return componentGroups.stream()
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(List::stream)
                .collect(toComponents());
    }

    @Override
    //todo maybe just make 2 private methods public instead of this flag check?
    public Component findComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor) {
        return mustBeDeclaredInEnvDescriptor
                ? getComponent(componentName)
                : findOrCreateComponent(componentName);
    }

    private Component getComponent(String componentName) {
        return firstFirstResult(componentGroups, g -> g.findComponentWithName(componentName))
                .orElseThrow(() -> new IllegalArgumentException(notFoundComponentMessage(componentName)));
    }

    private Component findOrCreateComponent(String componentName) {
        return firstFirstResult(componentGroups, g -> g.findComponentWithName(componentName))
                .orElseGet(() -> componentFactory.createComponent(componentName, componentName, name));
    }

    @Override
    public Components inGroups(List<String> groups) {
        if (groups.isEmpty()) return getAllComponents();

        return groups.stream()
                .map(this::findGroupWithName)
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(List::stream)
                .collect(toComponents());
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
