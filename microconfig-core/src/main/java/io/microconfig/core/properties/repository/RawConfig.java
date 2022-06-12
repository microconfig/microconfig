package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.EnvProperty;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class RawConfig {
    private final List<Include> includes;
    private final List<Property> declaredProperties;

    public Map<String, Property> getBaseAndIncludedProperties(Function<Include, Map<String, Property>> includeResolver, List<String> profiles, String buildEnvironment) {
        Map<String, Property> filtered = filterProperties(profiles, buildEnvironment);
        if (includes.isEmpty()) return filtered;

        getIncludedPropertiesUsing(includeResolver).forEach(filtered::putIfAbsent);
        return filtered;
    }

    private Map<String, Property> filterProperties(List<String> profiles, String buildEnvironment) {
        Map<String, Property> props = new LinkedHashMap<>();
        // base properties go first
        declaredProperties.stream()
                .filter(p -> !(p instanceof EnvProperty))
                .forEach(p -> props.put(p.getKey(), p));

        List<EnvProperty> envProps = declaredProperties.stream()
                .filter(p -> p instanceof EnvProperty)
                .map(p -> (EnvProperty) p)
                .collect(toList());

        // then multi-line vars
        envProps.stream()
                .filter(EnvProperty::multiLineVar)
                .forEach(p -> props.put(p.getKey(), p));

        // then profiles
        envProps.stream()
                .filter(p -> p.getEnvironment() != null)
                .filter(p -> profiles.contains(p.getEnvironment()))
                .forEach(p -> props.put(p.getKey(), p));

        // and then env specific
        envProps.stream()
                .filter(p -> p.getEnvironment() != null)
                .filter(p -> p.getEnvironment().equals(buildEnvironment))
                .forEach(p -> props.put(p.getKey(), p));

        return props;
    }

    private Map<String, Property> getIncludedPropertiesUsing(Function<Include, Map<String, Property>> includeResolver) {
        return includes.stream().map(includeResolver)
                .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }
}