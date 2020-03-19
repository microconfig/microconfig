package io.microconfig.core.environments.impl;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Components;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentGroupImpl implements ComponentGroup {
    @Getter
    private final String name;
    private final String ip;
    @Getter
    private final Components components;

    @Override
    public Optional<String> getIp() {
        return ofNullable(ip);
    }

    @Override
    public Optional<Component> findComponentWithName(String componentName) {
        return components.asList()
                .stream()
                .filter(c -> c.getName().equals(componentName))
                .findFirst();
    }

    @Override
    public String toString() {
        return name + ": " + components;
    }
}