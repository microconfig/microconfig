package io.microconfig.core.environments.impl;

import io.microconfig.core.environments.Component;

public interface ComponentFactory {
    Component createComponent(String name, String type, String environment);
}
