package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.impl.environments.ComponentFactory;
import io.microconfig.domain.impl.environments.ComponentGroupImpl;
import io.microconfig.domain.impl.properties.ComponentsImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;

import static io.microconfig.utils.CollectionUtils.join;
import static io.microconfig.utils.CollectionUtils.minus;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Collections.emptyList;

@With
@RequiredArgsConstructor
public class ComponentGroupDefinition {
    @Getter
    private final String name;
    @Getter
    private final String ip;
    @Getter
    private final List<ComponentDefinition> components;

    private final List<ComponentDefinition> excludedComponents;
    private final List<ComponentDefinition> appendedComponents;

    public ComponentGroupDefinition overrideBy(ComponentGroupDefinition override) {
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

    public ComponentGroup toGroup(ComponentFactory componentFactory, String environment) {
        return new ComponentGroupImpl(
                name,
                ip,
                new ComponentsImpl(
                        forEach(components, c -> componentFactory.createComponent(c.getName(), c.getType(), environment))
                )
        );
    }
}
