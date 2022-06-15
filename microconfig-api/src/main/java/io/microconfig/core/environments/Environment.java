package io.microconfig.core.environments;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface Environment {
    String getName();

    File getSource();

    boolean isAbstractEnv();

    int getPortOffset();

    List<String> getProfiles();

    List<ComponentGroup> getGroups();

    List<ComponentGroup> findGroupsWithIp(String ip);

    ComponentGroup getGroupWithName(String groupName);

    Optional<ComponentGroup> findGroupWithComponent(String componentName);

    Components getAllComponents();

    Component getComponentWithName(String componentName);

    Components findComponentsFrom(List<String> groups, List<String> components);

    Component findComponentWithName(String componentName);
}