package io.microconfig.configs.files.parser;

import io.microconfig.configs.Property;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ParsedComponent {
    private final String component;

    private final List<Include> includes;
    private final List<Property> properties;

    public List<Include> getIncludes() {
        return includes;
    }

    public List<Property> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return component;
    }
}