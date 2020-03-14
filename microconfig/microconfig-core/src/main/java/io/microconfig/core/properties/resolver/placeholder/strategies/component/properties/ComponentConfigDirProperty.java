package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.io.tree.ComponentTree;
import io.microconfig.core.properties.resolver.placeholder.strategies.component.ComponentProperty;
import io.microconfig.io.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class ComponentConfigDirProperty implements ComponentProperty {
    private final ComponentTree componentTree;

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> value(Component component) {
        return componentTree.getFolder(component.getType())
                .map(File::getAbsolutePath)
                .map(StringUtils::unixLikePath);
    }
}