package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ResultComponent;

public interface PropertiesProvider {
    ResultComponent buildProperties(String componentName, String componentType, String env);
}