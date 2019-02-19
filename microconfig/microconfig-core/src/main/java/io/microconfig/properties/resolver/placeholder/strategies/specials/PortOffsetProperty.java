package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolverStrategy.SpecialProperty;

import java.util.Optional;

public class PortOffsetProperty implements SpecialProperty {
    @Override
    public String key() {
        return "portOffset";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getPortOffset().map(Object::toString);
    }
}
