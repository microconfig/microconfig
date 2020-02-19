package io.microconfig.properties;

import io.microconfig.environments.Component;

public interface PropertySource {
    Component getComponent();

    default String sourceInfo() {
        return toString();
    }
}