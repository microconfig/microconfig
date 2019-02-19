package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolverStrategy.SpecialProperty;

import java.io.File;
import java.util.Optional;

import static java.util.Optional.of;

public class ConfigDir implements SpecialProperty {
    private final String configRoot;

    public ConfigDir(File configRoot) {
        this.configRoot = configRoot.getAbsolutePath();
    }

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return of(configRoot);
    }
}