package io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class IpProperty implements EnvProperty {
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
