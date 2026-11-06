package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.io.yaml.YamlScalars;
import io.microconfig.core.properties.io.yaml.YamlTreeImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class GroupedProperty implements Property {
    @Getter
    private final String key;
    private final Collection<Property> children;
    @Getter
    private final DeclaringComponent declaringComponent;

    @Override
    public String getValue() {
        return toYaml(children);
    }

    @Override
    public boolean isVar() {
        return false;
    }

    @Override
    public ConfigFormat getConfigFormat() {
        return YAML;
    }

    @Override
    public Property resolveBy(Resolver resolver, DeclaringComponent root) {
        List<Property> resolved = resolveChildren(resolver, root);
        return property(key, toYaml(resolved), YAML, declaringComponent);
    }

    private List<Property> resolveChildren(Resolver resolver, DeclaringComponent root) {
        return children.stream().map(p -> p.resolveBy(resolver, root)).collect(toList());
    }

    private String toYaml(Collection<Property> properties) {
        Map<String, String> values = properties.stream()
                .collect(toLinkedMap(p -> p.getKey().substring(key.length() + 1), YamlScalars::serializedValue));
        return new YamlTreeImpl(false).toYaml(values);
    }
}
