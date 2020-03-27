package io.microconfig.core.properties;

public interface Property {
    String getKey();

    String getValue();

    boolean isTemp();

    ConfigFormat getConfigFormat();

    DeclaringComponent getDeclaringComponent();

    Property resolveBy(Resolver resolver, DeclaringComponent root);
}