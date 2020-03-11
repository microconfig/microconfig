package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigBuildResult;

public interface PropertiesProvider {
    ConfigBuildResult buildProperties(String componentName, String componentType, String env);
}