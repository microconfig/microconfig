package io.microconfig.domain;

public interface PropertySource {
    //todo
    String getDeclaringComponentName();

    String getDeclaringComponentType();

    default String sourceInfo() {
        return toString();
    }
}