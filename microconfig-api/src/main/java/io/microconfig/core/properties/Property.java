package io.microconfig.core.properties;

public interface Property {
    String getKey();

    String getValue();

    boolean isVar();

    /** Whether the value contains literal YAML block content rather than a YAML fragment. */
    default boolean isLiteralText() {
        return false;
    }

    ConfigFormat getConfigFormat();

    DeclaringComponent getDeclaringComponent();

    Property resolveBy(Resolver resolver, DeclaringComponent root);
}
