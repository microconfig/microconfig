package io.microconfig.service.ioservice.selector;

public enum ConfigFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}