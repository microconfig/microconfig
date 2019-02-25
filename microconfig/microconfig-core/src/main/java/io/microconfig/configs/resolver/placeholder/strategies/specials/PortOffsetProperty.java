package io.microconfig.configs.resolver.placeholder.strategies.specials;

import io.microconfig.configs.resolver.placeholder.strategies.SpecialPropertyResolveStrategy.SpecialProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;

import java.util.Optional;

public class PortOffsetProperty implements SpecialProperty {
    @Override
    public String key() {
        return "portOffset";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return environment.getPortOffset()
                .map(Object::toString);
    }
}
