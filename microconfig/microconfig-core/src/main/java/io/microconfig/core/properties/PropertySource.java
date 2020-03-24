package io.microconfig.core.properties;

public interface PropertySource {
    String getConfigType();

    String getComponent();

    String getEnvironment();
}