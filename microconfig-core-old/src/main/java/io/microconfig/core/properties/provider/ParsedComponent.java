package io.microconfig.core.properties.provider;

import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;


@RequiredArgsConstructor
public class ParsedComponent {
    private final List<Include> includes;
    private final List<Property> properties;

    public List<Include> getIncludes() {
        return includes;
    }

    public Map<String, Property> getPropertiesAsMas() {
        return properties.stream()
                .collect(toMap(Property::getKey, p -> p));
    }
}