package io.microconfig.core.properties;

import io.microconfig.core.environments.Component;

public interface PropertySource {
    Component getComponent();

    default String sourceInfo() {
        return toString();
    }
}