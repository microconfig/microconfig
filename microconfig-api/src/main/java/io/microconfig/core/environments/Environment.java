package io.microconfig.core.environments;

import java.util.List;
import java.util.Optional;

public interface Environment {
    String getName();

    int getPortOffset();

    List<ComponentGroup> findGroupsWithIp(String ip);

    ComponentGroup getGroupWithName(String groupName);

    Optional<ComponentGroup> findGroupWithComponent(String componentName);

    Component getComponentWithName(String componentName);

    Component getOrCreateComponentWithName(String componentName);

    Components findComponentsFrom(List<String> groups, List<String> components);

    Components getAllComponents();
}