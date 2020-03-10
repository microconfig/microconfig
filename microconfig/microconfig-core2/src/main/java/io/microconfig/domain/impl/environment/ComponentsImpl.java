package io.microconfig.domain.impl.environment;

import io.microconfig.domain.BuildPropertiesStep;
import io.microconfig.domain.Component;
import io.microconfig.domain.Components;
import io.microconfig.domain.impl.properties.CompositeBuildPropertiesStep;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.StreamUtils.map;

@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public BuildPropertiesStep buildProperties() {
        return new CompositeBuildPropertiesStep(map(components, Component::buildProperties));
    }
}