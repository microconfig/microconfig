package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.ComponentFactory;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.impl.environments.ComponentGroupImpl;
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
class ComponentGroupDefinition {
    @Getter
    private final String name;
    @Getter
    private final String ip;
    @Getter
    private final List<ComponentDefinition> components;

    private final List<ComponentDefinition> excludedComponents;
    private final List<ComponentDefinition> appendedComponents;

    public ComponentGroupDefinition overrideBy(ComponentGroupDefinition override) {
        return getIpFrom(override)
                .getComponentsFrom(override)
                .getExcludedGroupsFrom(override)
                .getAppendedGroupsFrom(override);
    }

    private ComponentGroupDefinition getIpFrom(ComponentGroupDefinition override) {
        return override.ip == null ? this : withIp(override.ip);
    }

    private ComponentGroupDefinition getComponentsFrom(ComponentGroupDefinition override) {
        return override.components.isEmpty() ? this : withComponents(override.components);
    }

    private ComponentGroupDefinition getExcludedGroupsFrom(ComponentGroupDefinition override) {
        return override.excludedComponents.isEmpty() ? this : excludeComponents(override.excludedComponents);
    }

    private ComponentGroupDefinition getAppendedGroupsFrom(ComponentGroupDefinition override) {
        return override.appendedComponents.isEmpty() ? this : joinComponentsWith(override.appendedComponents);
    }

    private ComponentGroupDefinition excludeComponents(List<ComponentDefinition> toExclude) {
        return withComponents(minus(components, toExclude))
                .withExcludedComponents(emptyList());
    }

    private ComponentGroupDefinition joinComponentsWith(List<ComponentDefinition> newAppendedComponents) {
        return withComponents(join(components, newAppendedComponents))
                .withAppendedComponents(emptyList());
    }

    public ComponentGroup toGroup(ComponentFactory componentFactory, String environment) {
        return new ComponentGroupImpl(
                name,
                ip,
                componentFactory.toComponents(
                        forEach(components, c -> componentFactory.createComponent(c.getName(), c.getType(), environment))
                )
        );
    }
}
