package io.microconfig.core.properties;

public interface PropertySource {
    //todo
    String getDeclaringComponentName();

    String getDeclaringComponentType();

    default String sourceInfo() {
        return toString();
    }
}