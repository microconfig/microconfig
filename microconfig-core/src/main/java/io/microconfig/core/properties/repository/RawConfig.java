package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.OverrideProperty;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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
        Map<String, Property> propsByKey = declaredProperties.stream()
                .filter(p -> !isOverrideProperty(p))
                .collect(rejectingDuplicates(""));

        override(propsByKey, byMultilineVars(), " for all envs");
        for (String profile : profiles) {
            overrideFor(propsByKey, profile);
        }
        overrideFor(propsByKey, env);

        return propsByKey;
    }

    private boolean isOverrideProperty(Property p) {
        return p instanceof OverrideProperty;
    }

    private Collector<Property, ?, Map<String, Property>> rejectingDuplicates(String scope) {
        return toMap(Property::getKey, identity(), (p1, p2) -> {
            throw new IllegalStateException(format("Property '%s' is declared twice%s: %s and %s",
                    p1.getKey(), scope, p1.getDeclaringComponent(), p2.getDeclaringComponent()));
        }, LinkedHashMap::new);
    }

    private Predicate<OverrideProperty> byMultilineVars() {
        return p -> p.isVar() && p.isForAllEnvs();
    }

    // a block for a single env (+prod-a:) wins over a block shared between envs (+prod-a,prod-b:)
    private void overrideFor(Map<String, Property> propsByKey, String env) {
        String scope = " for env '" + env + "'";
        override(propsByKey, p -> p.isSharedWith(env), scope);
        override(propsByKey, p -> p.isOnlyFor(env), scope);
    }

    private void override(Map<String, Property> propsByKey, Predicate<OverrideProperty> predicate, String scope) {
        Map<String, Property> overrides = declaredProperties.stream()
                .filter(this::isOverrideProperty)
                .map(p -> (OverrideProperty) p)
                .filter(predicate)
                .collect(rejectingDuplicates(scope));
        propsByKey.putAll(overrides);
    }

    private Map<String, Property> getIncludedPropertiesUsing(Function<Include, Map<String, Property>> includeResolver) {
        return includes.stream().map(includeResolver)
                .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }
}
