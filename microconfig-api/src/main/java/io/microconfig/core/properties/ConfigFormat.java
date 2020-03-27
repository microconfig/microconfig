package io.microconfig.core.properties;

public enum ConfigFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return '.' + name().toLowerCase();
    }
}