package io.microconfig.properties.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class PortOffsetProperty implements EnvProperty {
    @Override
    public String key() {
        return "portOffset";
    }

    @Override
    public Optional<String> value(Component ignore, Environment environment) {
        return environment.getPortOffset()
                .map(Object::toString);
    }
}
