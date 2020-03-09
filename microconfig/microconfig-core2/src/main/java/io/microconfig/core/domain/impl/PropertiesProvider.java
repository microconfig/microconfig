package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.ComponentProperties;

public interface PropertiesProvider {
    ComponentProperties buildProperties(String componentName, String componentType, String env);
}