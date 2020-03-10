package io.microconfig.domain.impl.environment;

import io.microconfig.domain.BuildPropertiesStep;
import io.microconfig.domain.Component;
import io.microconfig.domain.Components;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public BuildPropertiesStep buildProperties() {
        return new CompositeBuildPropertiesStep(
                components.stream()
                        .map(Component::buildProperties)
                        .collect(toList())
        );
    }
}