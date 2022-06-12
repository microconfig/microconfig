package io.microconfig.core.properties;

public interface Property {
    String getKey();

    String getValue();

    boolean isVar();

    ConfigFormat getConfigFormat();

    DeclaringComponent getDeclaringComponent();

    Property resolveBy(Resolver resolver, DeclaringComponent root);
}