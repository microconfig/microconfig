package io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy.EnvProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.Environment;

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
