package io.microconfig.configs.resolver.placeholder.strategies.specials.general;

import io.microconfig.configs.resolver.placeholder.strategies.GeneralPropertiesResolveStrategy.GeneralProperty;
import io.microconfig.environments.Component;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class ConfigDir implements GeneralProperty {
    private final String configRoot;

    public ConfigDir(File configRoot) {
        this.configRoot = unixLikePath(configRoot.getAbsolutePath());
    }

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> value(Component ignore) {
        return of(configRoot);
    }
}