package io.microconfig.core.environments;

import java.util.List;

public interface Environment {
    String getName();

    int getPortOffset();

    List<ComponentGroup> findGroupsWithIp(String ip);

    ComponentGroup findGroupWithName(String groupName);

    ComponentGroup findGroupWithComponent(String componentName);

    Component getComponentWithName(String componentName);

    Component getOrCreateComponentWithName(String componentName);

    Components findComponentsFrom(List<String> groups, List<String> components);

    Components getAllComponents();
}