package io.microconfig.domain.impl.environments;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Components;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ComponentGroupImpl implements ComponentGroup {
    @Getter
    private final String name;
    private final String ip;
    private final String env;

    private final List<Component> components;

    @Override
    public Optional<String> getIp() {
        return ofNullable(ip);
    }

    @Override
    public Optional<Component> findComponentWithName(String componentName) {
        return components.stream()
                .filter(c -> c.getName().equals(componentName))
                .findFirst();
    }

    @Override
    public Components getAllComponents() {
        return new ComponentsImpl(components);
    }
}