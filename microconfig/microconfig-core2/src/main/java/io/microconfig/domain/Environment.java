package io.microconfig.domain;

import java.util.List;

public interface Environment {
    String getName();

    List<ComponentGroup> getGroupsWithIp(String ip);

    ComponentGroup getGroupWithName(String groupName);

    ComponentGroup getGroupWithComponent(String componentName);

    Components getAllComponents();

    Component getComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor);

    Components findComponentsFrom(List<String> groups, List<String> components);
}
