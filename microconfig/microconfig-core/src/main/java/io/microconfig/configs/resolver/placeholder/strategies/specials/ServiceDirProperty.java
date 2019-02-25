package io.microconfig.configs.resolver.placeholder.strategies.specials;

import io.microconfig.configs.resolver.placeholder.strategies.SpecialPropertyResolveStrategy.SpecialProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class ServiceDirProperty implements SpecialProperty {
    private final File destinationComponentDir;

    @Override
    public String key() {
        return "serviceDir";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        File dir = new File(destinationComponentDir, component.getName());
        return of(unixLikePath(dir.getAbsolutePath()));
    }
}