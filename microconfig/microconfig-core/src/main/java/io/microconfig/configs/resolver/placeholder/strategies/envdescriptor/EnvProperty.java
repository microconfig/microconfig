package io.microconfig.configs.resolver.placeholder.strategies.envdescriptor;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;

import java.util.Optional;

public interface EnvProperty {
    String key();

    Optional<String> value(Component component, Environment environment);
}
