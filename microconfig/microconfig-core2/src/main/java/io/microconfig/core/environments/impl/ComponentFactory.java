package io.microconfig.core.environments.impl;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Components;

import java.util.List;

public interface ComponentFactory {
    Component createComponent(String componentName, String componentType, String environment);

    Components toComponents(List<Component> components);
}