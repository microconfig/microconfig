package io.microconfig.core.properties;

public interface Property {
    String getKey();

    String getValue();

    PropertySource getSource();

    boolean isTemp();

    Property resolveBy(Resolver resolver, ComponentWithEnv root);
}