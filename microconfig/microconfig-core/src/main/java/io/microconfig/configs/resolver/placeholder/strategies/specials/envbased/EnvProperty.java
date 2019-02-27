package io.microconfig.configs.resolver.placeholder.strategies.specials.envbased;

import io.microconfig.configs.resolver.placeholder.strategies.EnvSpecificResolveStrategy;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;

import java.util.Optional;

import static java.util.Optional.of;

public class EnvProperty implements EnvSpecificResolveStrategy.EnvProperty {
    @Override
    public String key() {
        return "env";
    }

    @Override
    public Optional<String> value(Component ignore, Environment environment) {
        return of(environment.getName());
    }
}
