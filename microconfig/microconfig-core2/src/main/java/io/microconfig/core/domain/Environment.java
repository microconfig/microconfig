package io.microconfig.core.domain;

import java.util.List;

public interface Environment {
    String getName();

    ComponentGroup getGroupByName(String groupName);

    ComponentGroup getGroupByComponentName(String componentName);


    List<Component> getAllComponents();

    Component getComponentByName(String componentName);
}
