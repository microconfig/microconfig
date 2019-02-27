package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentResolveStrategy.ComponentProperty;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.utils.FileUtils.userHomeString;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

//todo ${system@user.home}
@RequiredArgsConstructor
public class UserHomeProperty implements ComponentProperty {
    @Override
    public String key() {
        return "userHome";
    }

    @Override
    public Optional<String> value(Component component) {
        return of(unixLikePath(userHomeString()));
    }
}