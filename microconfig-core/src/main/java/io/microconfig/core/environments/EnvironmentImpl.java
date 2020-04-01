package io.microconfig.core.environments;

import io.microconfig.core.properties.PropertiesFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.microconfig.utils.Logger.info;
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
    private final List<ComponentGroup> groups;

    private final ComponentFactory componentFactory;
    private final PropertiesFactory propertiesFactory;

    @Override
    public List<ComponentGroup> findGroupsWithIp(String ip) {
        return filter(groups, g -> g.getIp().filter(ip::equals).isPresent());
    }

    @Override
    public ComponentGroup getGroupWithName(String groupName) {
        return findGroup(group -> group.getName().equals(groupName),
                () -> "groupName=" + groupName);
    }

    @Override
    public Optional<ComponentGroup> findGroupWithComponent(String componentName) {
        return groups.stream()
                .filter(g -> g.findComponentWithName(componentName).isPresent())
                .findFirst();
    }

    @Override
    public Components getAllComponents() {
        List<Component> components = groups.stream()
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(List::stream)
                .collect(toList());

        return new ComponentsImpl(components, propertiesFactory);
    }

    @Override
    public Component getComponentWithName(String componentName) {
        return findFirstResult(groups, g -> g.findComponentWithName(componentName))
                .orElseThrow(() -> new IllegalArgumentException(notFoundComponentMessage(componentName)));
    }

    @Override
    public Components findComponentsFrom(List<String> groups, List<String> componentNames) {
        List<Component> componentsFromGroups = componentsFrom(groups);
        List<Component> result = filterBy(componentNames, componentsFromGroups);
        info("Filtered " + result.size() + " component(s) in [" + name + "] env.");
        return new ComponentsImpl(result, propertiesFactory);
    }

    @Override
    public Component getOrCreateComponentWithName(String componentName) {
        return findFirstResult(groups, g -> g.findComponentWithName(componentName))
                .orElseGet(() -> componentFactory.createComponent(componentName, componentName, name));
    }

    private List<Component> componentsFrom(List<String> groups) {
        if (groups.isEmpty()) return getAllComponents().asList();

        return groups.stream()
                .map(this::getGroupWithName)
                .map(ComponentGroup::getComponents)
                .map(Components::asList)
                .flatMap(List::stream)
                .collect(toList());
    }

    private List<Component> filterBy(List<String> components, List<Component> componentFromGroups) {
        if (components.isEmpty()) return componentFromGroups;

        Map<String, Component> componentByName = componentFromGroups.stream()
                .collect(toMap(Component::getName, identity()));
        return forEach(components, name -> requireNonNull(componentByName.get(name), () -> notFoundComponentMessage(name)));
    }

    private ComponentGroup findGroup(Predicate<ComponentGroup> groupPredicate,
                                     Supplier<String> description) {
        return groups.stream()
                .filter(groupPredicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find group by filter: '" + description.get() + "' in env '" + name + "'"));
    }

    private String notFoundComponentMessage(String component) {
        return "Component '" + component + "' is not configured for env '" + name + "'";
    }

    @Override
    public String toString() {
        return name + ": " + groups;
    }
}
