package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;

public interface ComponentFactory {
    Component createComponent(String componentAlias, String componentType, String environment);
}