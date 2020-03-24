package io.microconfig.core.properties;

public interface DeclaringComponent {
    String getConfigType();

    String getComponent();

    String getEnvironment();
}