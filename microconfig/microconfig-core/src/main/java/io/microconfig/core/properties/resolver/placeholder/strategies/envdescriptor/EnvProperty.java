package io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Environment;

import java.util.Optional;

public interface EnvProperty {
    String key();

    Optional<String> value(Component component, Environment environment);
}
