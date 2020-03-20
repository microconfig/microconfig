package io.microconfig.core.resolvers.placeholder.strategies.component.properties;

import io.microconfig.core.properties.impl.repository.ComponentGraph;
import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentProperty;
import io.microconfig.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class ConfigDirProperty implements ComponentProperty {
    private final ComponentGraph componentGraph;

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> resolveFor(String component) {
        return componentGraph.getFolderOf(getOriginalNameOf(component))
                .map(File::getAbsolutePath)
                .map(StringUtils::unixLikePath);
    }

    private String getOriginalNameOf(String component) {
        return component; //todo
    }
}