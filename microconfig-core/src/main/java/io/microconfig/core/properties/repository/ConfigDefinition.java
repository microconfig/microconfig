package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class ConfigDefinition {
    private final List<Include> includes;
    private final Map<String, Property> properties;

    public Map<String, Property> getBaseAndIncludedProperties(Function<Include, Map<String, Property>> includeResolver) {
        if (includes.isEmpty()) return properties;

        Map<String, Property> componentProperties = new LinkedHashMap<>(properties);
        getIncludedConfigs(includeResolver).forEach(componentProperties::putIfAbsent);
        return componentProperties;
    }

    private Map<String, Property> getIncludedConfigs(Function<Include, Map<String, Property>> includeResolver) {
        return includes.stream().map(includeResolver)
                .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }
}
