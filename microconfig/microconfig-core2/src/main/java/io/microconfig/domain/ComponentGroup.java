package io.microconfig.domain;

import java.util.Optional;

public interface ComponentGroup {
    String getName();

    Optional<String> getIp();

    Optional<Component> findComponentWithName(String name);

    Components getComponents();
}