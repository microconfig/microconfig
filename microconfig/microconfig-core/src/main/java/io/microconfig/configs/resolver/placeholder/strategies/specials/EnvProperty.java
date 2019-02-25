package io.microconfig.configs.resolver.placeholder.strategies.specials;

import io.microconfig.configs.resolver.placeholder.strategies.SpecialPropertyResolveStrategy.SpecialProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;

import java.util.Optional;

import static java.util.Optional.of;

public class EnvProperty implements SpecialProperty {
    @Override
    public String key() {
        return "env";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return of(environment.getName());
    }
}
