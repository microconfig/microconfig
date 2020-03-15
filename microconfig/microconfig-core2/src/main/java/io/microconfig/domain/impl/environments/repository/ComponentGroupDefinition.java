package io.microconfig.domain.impl.environments.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ComponentGroupDefinition {
    private final String name;
    private final String ip;

    private final List<ComponentDefinition> declaredComponents;
    private final List<ComponentDefinition> excludedComponents;
    private final List<ComponentDefinition> appendedComponents;
}
