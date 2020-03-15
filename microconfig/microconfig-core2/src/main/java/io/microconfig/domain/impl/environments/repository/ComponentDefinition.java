package io.microconfig.domain.impl.environments.repository;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ComponentDefinition {
    private final String name; //alias, must be unique among env
    private final String type;//folder name

    public static ComponentDefinition byNameAndType(String name, String type) {
        return new ComponentDefinition(name, type);
    }

    public static ComponentDefinition byType(String type) {
        return byNameAndType(type, type);
    }
}
