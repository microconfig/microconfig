package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import io.microconfig.configs.io.tree.ComponentTree;
import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentResolveStrategy.ComponentProperty;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ComponentPropertiesFactory {
    private final ComponentTree componentTree;
    private final File destinationComponentDir;

    public Map<String, ComponentProperty> get() {
        return of(
                new ComponentConfigDirProperty(componentTree),
                new ComponentNameProperty(),
                new ResultServiceDirProperty(destinationComponentDir),
                new ConfigRootDirProperty(componentTree.getRootDir())
        ).collect(toMap(ComponentProperty::key, identity()));
    }
}