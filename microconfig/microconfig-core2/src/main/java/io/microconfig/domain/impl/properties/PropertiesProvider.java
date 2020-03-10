package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ResolvedComponent;

public interface PropertiesProvider {
    ResolvedComponent buildProperties(String componentName, String componentType, String env);
}