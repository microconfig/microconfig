package io.microconfig.domain.impl.environments.repository;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ComponentGroupDefinition {
    private final String componentGroupName;
    private final String ip;
    private final List<ComponentDefinition> parsedComponents;
    private final List<ComponentDefinition> excludedComponents;
    private final List<ComponentDefinition> appendedComponents;
}
