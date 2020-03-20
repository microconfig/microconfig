package io.microconfig.core.properties;

public interface PropertySource {
    String getDeclaringComponent();

    default String sourceInfo() {
        return toString();
    }
}