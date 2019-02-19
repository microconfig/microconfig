package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialResolverStrategy.SpecialKey;

import java.util.Optional;

import static java.util.Optional.of;

public class FolderKey implements SpecialKey {
    @Override
    public String key() {
        return "folder";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return of("folder");
    }
}
