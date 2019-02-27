package io.microconfig.configs.resolver.placeholder.strategies.specials.general;

import io.microconfig.configs.resolver.placeholder.strategies.GeneralPropertiesResolveStrategy.GeneralProperty;
import io.microconfig.environments.Component;

import java.util.Optional;

import static io.microconfig.utils.FileUtils.userHomeString;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class UserHomeProperty implements GeneralProperty {
    @Override
    public String key() {
        return "userHome";
    }

    @Override
    public Optional<String> value(Component component) {
        return of(unixLikePath(userHomeString()));
    }
}