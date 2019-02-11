package io.microconfig.configs.properties;

import io.microconfig.configs.environment.Component;

import java.util.Map;

public interface PropertiesProvider {
    Map<String, Property> getProperties(Component component, String environment);
}
