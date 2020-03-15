package io.microconfig.domain.impl.properties.io;

public enum ConfigFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}