package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import io.microconfig.configs.files.tree.ComponentTree;
import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentResolveStrategy.ComponentProperty;
import io.microconfig.environments.Component;
import io.microconfig.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class ComponentFolderProperty implements ComponentProperty {
    private final ComponentTree componentTree;

    @Override
    public String key() {
        return "folder";
    }

    @Override
    public Optional<String> value(Component component) {
        return componentTree.getFolder(component.getType())
                .map(File::getAbsolutePath)
                .map(StringUtils::unixLikePath);
    }
}