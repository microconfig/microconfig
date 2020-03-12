package io.microconfig.service.io;

public enum ConfigFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}