package io.microconfig.domain.impl.environments;

import io.microconfig.domain.Component;

public interface ComponentFactory {
    Component createComponent(String componentName, String componentType, String environment);
}