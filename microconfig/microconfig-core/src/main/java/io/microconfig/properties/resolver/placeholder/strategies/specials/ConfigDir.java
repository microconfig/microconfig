package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialResolverStrategy.SpecialKey;

import java.util.Optional;

import static io.microconfig.utils.FileUtils.userHomeString;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class ConfigDir implements SpecialKey {
    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return of("");
    }
}
