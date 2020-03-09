package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class EnvironmentImpl implements Environment {
    @Getter
    private final String name;
    private final List<ComponentGroup> componentGroups;

    @Override
    public ComponentGroup getGroupByName(String groupName) {
        List<ComponentGroup> groups = componentGroups.stream()
                .filter(g -> g.getName().equals(groupName))
                .collect(toList());

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Can't find group '" + groupName + "' in env [" + name + "]");
        }

        return singleValue(groups);
    }

    @Override
    public ComponentGroup getGroupByComponentName(String componentName) {
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
    public Component getComponentByName(String componentName) {
        return componentGroups.stream()
                .filter(g -> g.containsComponent(componentName))
                .map(g -> g.getComponentByName(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find component '" + componentName + "' in env [" + name + "]"));
    }
}
