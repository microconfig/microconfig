package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentResolveStrategy.ComponentProperty;
import io.microconfig.environments.Component;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class ConfigRootDirProperty implements ComponentProperty {
    private final String configRoot;

    public ConfigRootDirProperty(File configRoot) {
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