package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class RawConfig {
    private final List<Include> includes;
    private final Map<String, Property> declaredProperties;

    public Map<String, Property> getBaseAndIncludedProperties(Function<Include, Map<String, Property>> includeResolver) {
        if (includes.isEmpty()) return declaredProperties;

        Map<String, Property> allProperties = new LinkedHashMap<>(declaredProperties);
        getIncludedPropertiesUsing(includeResolver).forEach(allProperties::putIfAbsent);
        return allProperties;
    }

    private Map<String, Property> getIncludedPropertiesUsing(Function<Include, Map<String, Property>> includeResolver) {
        return includes.stream().map(includeResolver)
                .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }
}