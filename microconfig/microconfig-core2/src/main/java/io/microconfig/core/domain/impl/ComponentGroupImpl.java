package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.Component;
import io.microconfig.core.domain.ComponentGroup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ComponentGroupImpl implements ComponentGroup {
    @Getter
    private final String name;
    private final String type;

    @Getter
    private final List<Component> components;

    @Override
    public boolean containsComponent(String componentName) {
        return findComponentByName(componentName).isPresent();
    }

    @Override
    public Component getComponentByName(String name) {
        return findComponentByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Can't find component '" + name + "' in group [" + name + "]"));
    }

    private Optional<Component> findComponentByName(String componentName) {
        return components.stream()
                .filter(c -> c.getName().equals(componentName))
                .findFirst();
    }
}
