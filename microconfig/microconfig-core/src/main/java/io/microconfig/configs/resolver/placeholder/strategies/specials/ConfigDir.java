package io.microconfig.configs.resolver.placeholder.strategies.specials;

import io.microconfig.configs.resolver.placeholder.strategies.SpecialPropertyResolveStrategy.SpecialProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class ConfigDir implements SpecialProperty {
    private final String configRoot;

    public ConfigDir(File configRoot) {
        this.configRoot = unixLikePath(configRoot.getAbsolutePath());
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