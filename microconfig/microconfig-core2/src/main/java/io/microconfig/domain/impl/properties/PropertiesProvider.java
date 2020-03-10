package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ResolvedProperties;

public interface PropertiesProvider {
    ResolvedProperties buildProperties(String componentName, String componentType, String env);
}