package io.microconfig.core.properties;

import io.microconfig.core.environments.Component;

import java.util.Map;

public interface ConfigProvider {
    Map<String, Property> getProperties(Component component, String environment);
}