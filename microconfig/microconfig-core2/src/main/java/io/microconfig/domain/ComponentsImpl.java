package io.microconfig.domain;

import io.microconfig.domain.impl.environment.Components;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public ComponentResolver resolveProperties() {
        return null;
    }
}
