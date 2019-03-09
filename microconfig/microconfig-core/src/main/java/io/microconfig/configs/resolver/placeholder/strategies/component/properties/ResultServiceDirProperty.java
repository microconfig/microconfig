package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentResolveStrategy.ComponentProperty;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class ResultServiceDirProperty implements ComponentProperty {
    private final File destinationComponentDir;

    @Override
    public String key() {
        return "resultDir";
    }

    @Override
    public Optional<String> value(Component component) {
        File dir = new File(destinationComponentDir, component.getName());
        return of(unixLikePath(dir.getAbsolutePath()));
    }
}