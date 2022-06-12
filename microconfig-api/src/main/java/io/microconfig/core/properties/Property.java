package io.microconfig.core.properties;

public interface Property {
    String getKey();

    String getValue();

    boolean isVar();

    default String getEnvironment() {
        return null;
    };

    ConfigFormat getConfigFormat();

    DeclaringComponent getDeclaringComponent();

    Property resolveBy(Resolver resolver, DeclaringComponent root);
}