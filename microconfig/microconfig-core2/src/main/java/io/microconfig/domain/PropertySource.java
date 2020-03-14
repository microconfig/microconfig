package io.microconfig.domain;

public interface PropertySource {
    String getDeclaringComponentName();

    String getDeclaringComponentType();

    default String sourceInfo() {
        return toString();
    }
}