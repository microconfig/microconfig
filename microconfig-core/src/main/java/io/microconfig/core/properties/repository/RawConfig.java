package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.OverrideProperty;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.function.Function.identity;

@RequiredArgsConstructor
public class RawConfig {
    private final List<Include> includes;
    private final List<Property> declaredProperties;

    public Map<String, Property> getBaseAndIncludedProperties(Function<Include, Map<String, Property>> includeResolver,
                                                              List<String> profiles,
                                                              String env) {
        Map<String, Property> filtered = filterProperties(profiles, env);
        if (includes.isEmpty()) return filtered;

        getIncludedPropertiesUsing(includeResolver).forEach(filtered::putIfAbsent);
        return filtered;
    }

    private Map<String, Property> filterProperties(List<String> profiles, String env) {
        // base properties go first
        Map<String, Property> propsByKey = filter(declaredProperties, p -> !isOverrideProperty(p), toLinkedMap(Property::getKey, identity()));

        override(propsByKey, byMultilineVars());
        override(propsByKey, byPropertiesForProfile(profiles));
        override(propsByKey, byPropertiesForEnv(env));

        return propsByKey;
    }

    private Predicate<OverrideProperty> byMultilineVars() {
        return p -> p.isVar() && p.getEnvironment() == null;
    }

    private Predicate<OverrideProperty> byPropertiesForProfile(List<String> profiles) {
        return p -> p.getEnvironment() != null && profiles.contains(p.getEnvironment());
    }

    private Predicate<OverrideProperty> byPropertiesForEnv(String env) {
        return p -> p.getEnvironment() != null && env.equals(p.getEnvironment());
    }

    private void override(Map<String, Property> propsByKey, Predicate<OverrideProperty> predicate) {
        Map<String, Property> overrides = declaredProperties.stream()
                .filter(this::isOverrideProperty)
                .map(p -> (OverrideProperty) p)
                .filter(predicate)
                .collect(toLinkedMap(Property::getKey, identity()));
        propsByKey.putAll(overrides);
    }

    private boolean isOverrideProperty(Property p) {
        return p instanceof OverrideProperty;
    }

    private Map<String, Property> getIncludedPropertiesUsing(Function<Include, Map<String, Property>> includeResolver) {
        return includes.stream().map(includeResolver)
                .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }
}