package io.microconfig.configs.resolver.placeholder.strategies.specials;

import io.microconfig.configs.resolver.placeholder.strategies.SpecialPropertyResolveStrategy.SpecialProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.Environment;

import java.util.Optional;

public class GroupProperty implements SpecialProperty {
    @Override
    public String key() {
        return "group";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getGroupByComponentName(component.getName())
                .map(ComponentGroup::getName);
    }
}
