package io.microconfig.core.environments;

import io.microconfig.core.properties.Component;
import io.microconfig.core.properties.Components;

import java.util.Optional;

public interface ComponentGroup {
    String getName();

    Optional<String> getIp();

    Optional<Component> findComponentWithName(String name);

    Components getComponents();
}