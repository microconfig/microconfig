package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolveStrategy.SpecialProperty;

import java.util.Optional;

public class OrderProperty implements SpecialProperty {
    @Override
    public String key() {
        return "order";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getGroupByComponentName(component.getName())
                .map(cg -> 1 + cg.getComponentNames().indexOf(component.getName()))
                .map(String::valueOf);
    }
}
