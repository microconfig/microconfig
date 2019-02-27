package io.microconfig.configs.resolver.placeholder.strategies.specials.general;

import io.microconfig.configs.resolver.placeholder.strategies.GeneralPropertiesResolveStrategy.GeneralProperty;
import io.microconfig.environments.Component;

import java.util.Optional;

import static java.util.Optional.of;

public class NameProperty implements GeneralProperty {
    @Override
    public String key() {
        return "name";
    }

    @Override
    public Optional<String> value(Component component) {
        return of(component.getName());
    }
}
