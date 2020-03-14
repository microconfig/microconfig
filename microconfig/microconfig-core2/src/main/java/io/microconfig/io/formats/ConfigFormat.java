package io.microconfig.io.formats;

public enum ConfigFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}