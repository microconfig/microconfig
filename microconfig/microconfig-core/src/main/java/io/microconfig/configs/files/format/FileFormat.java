package io.microconfig.configs.files.format;

public enum FileFormat {
    YAML,
    PROPERTIES;

    public String extension() {
        return "." + name().toLowerCase();
    }
}
