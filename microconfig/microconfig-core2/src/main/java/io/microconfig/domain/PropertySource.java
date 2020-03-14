package io.microconfig.domain;

public interface PropertySource {
    default String sourceInfo() {
        return toString();
    }
}