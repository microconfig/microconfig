package io.microconfig.core.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Optional;

public class IpProperty implements EnvProperty {
    @Override
    public String key() {
        return "ip";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
        return environment.findGroupWithComponent(component)
                .flatMap(ComponentGroup::getIp);
    }
}
