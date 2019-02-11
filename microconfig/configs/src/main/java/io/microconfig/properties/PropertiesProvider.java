package io.microconfig.properties;

import io.microconfig.environments.Component;

import java.util.Map;

public interface PropertiesProvider {
    Map<String, Property> getProperties(Component component, String environment);
}
