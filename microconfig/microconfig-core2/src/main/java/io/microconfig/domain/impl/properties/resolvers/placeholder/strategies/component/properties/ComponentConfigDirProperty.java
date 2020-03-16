package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component.properties;

import io.microconfig.domain.impl.properties.repository.ComponentGraph;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component.ComponentProperty;
import io.microconfig.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class ComponentConfigDirProperty implements ComponentProperty {
    private final ComponentGraph componentGraph;

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> value(String __, String componentType) {
        return componentGraph.getFolderOf(componentType)
                .map(File::getAbsolutePath)
                .map(StringUtils::unixLikePath);
    }
}