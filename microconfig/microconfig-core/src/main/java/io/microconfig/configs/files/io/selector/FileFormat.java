package io.microconfig.configs.files.io.selector;

public enum FileFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}
