package io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class IpProperty implements EnvProperty {
    @Override
    public String key() {
        return "ip";
    }

    @Override
    public Optional<String> resolveFor(String component, Environment environment) {
        return environment.findGroupWithComponent(component).getIp(); //todo
    }
}
