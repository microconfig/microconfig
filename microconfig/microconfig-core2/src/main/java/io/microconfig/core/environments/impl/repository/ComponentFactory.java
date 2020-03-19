package io.microconfig.core.environments.impl.repository;

import io.microconfig.core.environments.Component;

public interface ComponentFactory {
    Component createComponent(String name, String type, String environment);
}
