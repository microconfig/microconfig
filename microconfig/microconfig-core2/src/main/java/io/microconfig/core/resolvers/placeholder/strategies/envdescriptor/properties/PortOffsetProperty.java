package io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Optional;

public class PortOffsetProperty implements EnvProperty {
    @Override
    public String key() {
        return "portOffset";
    }

    @Override
    public Optional<String> value(String componentName, String componentType, Environment environment) {
//        return environment.getPortOffset()
//                .map(Object::toString);
        return Optional.empty();
    }
}
