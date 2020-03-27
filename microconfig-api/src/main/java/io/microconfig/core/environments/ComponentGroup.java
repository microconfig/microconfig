package io.microconfig.core.environments;

import java.util.Optional;

public interface ComponentGroup {
    String getName();

    Optional<String> getIp();

    Components getComponents();

    Optional<Component> findComponentWithName(String name);
}