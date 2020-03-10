package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static io.microconfig.utils.CollectionUtils.singleValue;
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
    public List<ComponentGroup> getGroupsWithIp(String ip) {
        return componentGroups.stream()
                .filter(g -> g.getIp().filter(ip::equals).isPresent())
                .collect(toList());
    }

    @Override
    public ComponentGroup getGroupWithName(String groupName) {
        List<ComponentGroup> groups = componentGroups.stream()
                .filter(g -> g.getName().equals(groupName))
                .collect(toList());

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Can't find group '" + groupName + "' in env [" + name + "]");
        }

        return singleValue(groups);
    }

    @Override
    public ComponentGroup getGroupWithComponent(String componentName) {
        return componentGroups.stream()
                .filter(g -> g.containsComponent(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find group by component '" + componentName + "' in env [" + name + "]"));
    }

    @Override
    public List<Component> getAllComponents() {
        return componentGroups.stream()
                .flatMap(group -> group.getComponents().stream())
                .collect(toList());
    }

    @Override
    public Component getComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor) {
        return componentGroups.stream()
                .filter(g -> g.containsComponent(componentName))
                .map(g -> g.getComponentByName(componentName))
                .findFirst()
                .orElseGet(() -> null);//todo
    }

    @Override
    public List<Component> findComponentsFrom(List<String> groups, List<String> components) {
        Supplier<List<Component>> componentsFromGroups = () -> {
            if (groups.isEmpty()) return getAllComponents();

            return groups.stream()
                    .map(this::getGroupWithName)
                    .flatMap(g -> g.getComponents().stream())
                    .collect(toList());
        };

        UnaryOperator<List<Component>> filterByComponents = (componentFromGroups) -> {
            if (components.isEmpty()) return componentFromGroups;

            Map<String, Component> componentByName = componentFromGroups.stream()
                    .collect(toMap(Component::getName, identity()));
            return components.stream()
                    .map(name -> requireNonNull(componentByName.get(name),
                            () -> "Component '" + name + "' is not configured for " + name + " env"))
                    .collect(toList());
        };

        List<Component> componentFromGroups = componentsFromGroups.get();
        return filterByComponents.apply(componentFromGroups);
    }
}
