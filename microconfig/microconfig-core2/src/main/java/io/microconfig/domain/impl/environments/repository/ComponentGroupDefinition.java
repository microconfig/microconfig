package io.microconfig.domain.impl.environments.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;

import static io.microconfig.io.CollectionUtils.join;
import static io.microconfig.io.CollectionUtils.minus;
import static java.util.Collections.emptyList;

@Getter
@With
@RequiredArgsConstructor
public class ComponentGroupDefinition {
    private final String name;
    private final String ip;
    private final List<ComponentDefinition> components;

    private final List<ComponentDefinition> excludedComponents;
    private final List<ComponentDefinition> appendedComponents;

    public ComponentGroupDefinition override(ComponentGroupDefinition override) {
        ComponentGroupDefinition result = this;

        if (override.ip != null) {
            result = result.withIp(override.ip);
        }
        if (!override.components.isEmpty()) {
            result = result.withComponents(override.components);
        }
        if (!override.excludedComponents.isEmpty()) {
            result = result.excludeComponents(override.excludedComponents);
        }
        if (!override.appendedComponents.isEmpty()) {
            result = result.appendComponents(override.appendedComponents);
        }

        return result;
    }

    private ComponentGroupDefinition excludeComponents(List<ComponentDefinition> toExclude) {
        return withComponents(minus(components, toExclude))
                .withExcludedComponents(emptyList());
    }

    private ComponentGroupDefinition appendComponents(List<ComponentDefinition> newAppendedComponents) {
        return withComponents(join(components, newAppendedComponents))
                .withAppendedComponents(emptyList());
    }
}
