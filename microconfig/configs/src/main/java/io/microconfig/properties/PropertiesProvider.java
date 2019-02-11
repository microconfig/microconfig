package io.microconfig.properties;

import io.microconfig.environment.Component;

import java.util.Map;

public interface PropertiesProvider {
    Map<String, Property> getProperties(Component component, String environment);
}
