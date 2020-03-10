package io.microconfig.domain;

import java.util.Optional;

public interface ComponentGroup {
    String getName();

    Optional<String> getIp();

    boolean containsComponent(String componentName);

    Component getComponentWithName(String name);

    Components getComponents();
}