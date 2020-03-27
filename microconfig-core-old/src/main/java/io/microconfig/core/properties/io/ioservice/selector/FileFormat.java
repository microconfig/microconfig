package io.microconfig.core.properties.io.ioservice.selector;

public enum FileFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}
