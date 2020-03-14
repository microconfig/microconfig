package io.microconfig.domain.impl.environment.provider;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigTypes;
import io.microconfig.domain.Resolver;
import io.microconfig.domain.impl.environment.ComponentFactory;
import io.microconfig.domain.impl.properties.FileBasedComponent;
import io.microconfig.domain.impl.properties.ResolvableComponent;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypes configTypes;
    private final FileSystemGraph fileSystemGraph;
    private final Resolver resolver;

    @Override
    public Component createComponent(String componentName, String environment) {
        return new ResolvableComponent(
                new FileBasedComponent(
                        configTypes,
                        fileSystemGraph,
                        componentName,
                        environment
                ),
                resolver
        );
    }
}