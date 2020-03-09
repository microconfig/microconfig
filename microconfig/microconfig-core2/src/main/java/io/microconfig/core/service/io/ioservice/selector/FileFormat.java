package io.microconfig.core.service.io.ioservice.selector;

public enum FileFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}
