package io.microconfig.core.properties.impl.io;

public enum ConfigFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}