package io.microconfig.core.domain;

import java.util.List;

public interface ComponentGroup {
    String getName();

    boolean containsComponent(String componentName);

    Component getComponentByName(String name);

    List<Component> getComponents();
}