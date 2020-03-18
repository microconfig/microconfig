package io.microconfig.domain;

import java.util.List;

public interface Environment {
    String getName();

    List<ComponentGroup> findGroupsWithIp(String ip);

    ComponentGroup findGroupWithName(String groupName);

    ComponentGroup findGroupWithComponent(String componentName);

    Component findComponentWithName(String componentName, boolean mustBeDeclaredInEnvDescriptor);

    Components inGroups(List<String> groups);

    Components getAllComponents();
}
