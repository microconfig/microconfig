package io.microconfig.domain;

import java.util.List;
import java.util.Optional;

public interface ComponentGroup {
    String getName();

    Optional<String> getIp();

    boolean containsComponent(String componentName);

    Component getComponentByName(String name);

    List<Component> getComponents();
}