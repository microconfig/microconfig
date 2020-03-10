package io.microconfig.domain;

import io.microconfig.domain.impl.environment.Components;

import java.util.Optional;

public interface ComponentGroup {
    String getName();

    Optional<String> getIp();

    boolean containsComponent(String componentName);

    Component getComponentWithName(String name);

    Components getComponents();
}