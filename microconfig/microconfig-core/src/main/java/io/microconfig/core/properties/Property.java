package io.microconfig.core.properties;

public interface Property {
    String getKey();

    String getValue();

    boolean isTemp();

    PropertySource getSource();

    Property resolveBy(Resolver resolver, ComponentWithEnv root);
}