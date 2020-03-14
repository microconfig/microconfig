package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.Property;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


@RequiredArgsConstructor
class ParsedComponent {
    @Getter
    private final List<Include> includes;
    private final List<Property> properties;

    public Map<String, Property> getPropertiesAsMas() {
        return properties.stream()
                .collect(toMap(Property::getKey, identity()));
    }
}