package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ComponentProperties;

public interface PropertiesProvider {
    ComponentProperties buildProperties(String componentName, String componentType, String env);
}