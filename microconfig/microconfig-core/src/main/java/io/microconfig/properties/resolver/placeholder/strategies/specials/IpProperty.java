package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolverStrategy.SpecialProperty;

import java.util.Optional;

public class IpProperty implements SpecialProperty {
    @Override
    public String key() {
        return "ip";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getGroupByComponentName(component.getName())
                .flatMap(ComponentGroup::getIp);
    }
}
