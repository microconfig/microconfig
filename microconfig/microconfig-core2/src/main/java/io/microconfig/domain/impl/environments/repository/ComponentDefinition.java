package io.microconfig.domain.impl.environments.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ComponentDefinition {
    private final String name;
    private final String type;

    public static ComponentDefinition withAlias(String alias, String type) {
        return new ComponentDefinition(alias, type);
    }

    public static ComponentDefinition withName(String name) {
        return withAlias(name, name);
    }
}
