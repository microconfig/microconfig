package io.microconfig.core.environments.impl.repository;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
class ComponentDefinition {
    private final String name;
    private final String type;

    public static ComponentDefinition withAlias(String alias, String type) {
        return new ComponentDefinition(alias, type);
    }

    public static ComponentDefinition withName(String name) {
        return withAlias(name, name);
    }
}
