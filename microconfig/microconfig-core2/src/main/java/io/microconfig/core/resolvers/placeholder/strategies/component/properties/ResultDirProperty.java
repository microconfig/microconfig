package io.microconfig.core.resolvers.placeholder.strategies.component.properties;

import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentProperty;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class ResultDirProperty implements ComponentProperty {
    private final File destinationComponentDir;

    @Override
    public String key() {
        return "resultDir";
    }

    @Override
    public Optional<String> resolveFor(String component) {
        File dir = new File(destinationComponentDir, component);
        return of(unixLikePath(dir.getAbsolutePath()));
    }
}