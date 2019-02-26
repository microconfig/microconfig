package io.microconfig.configs.files.parser;

import io.microconfig.configs.Property;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ParsedComponent {
    private final String component;

    @Getter
    private final List<Include> includes;
    @Getter
    private final List<Property> properties;

    @Override
    public String toString() {
        return component;
    }
}