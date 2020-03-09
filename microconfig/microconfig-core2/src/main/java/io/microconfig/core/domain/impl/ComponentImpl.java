package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.Component;
import io.microconfig.core.domain.ComponentProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final String component;

    @Override
    public String getName() {
        return component;
    }

    @Override
    public List<ComponentProperties> buildPropertiesForEachConfigType() {
        return null;
    }

    @Override
    public ComponentProperties buildPropertiesForType(String configType) {
        return null;
    }
}
