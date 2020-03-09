package io.microconfig.domain.impl.properties;

public interface PropertySource {
    default String sourceInfo() {
        return toString();
    }
}