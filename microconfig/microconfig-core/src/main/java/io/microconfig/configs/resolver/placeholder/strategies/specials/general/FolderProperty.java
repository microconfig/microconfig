package io.microconfig.configs.resolver.placeholder.strategies.specials.general;

import io.microconfig.configs.files.tree.ComponentTree;
import io.microconfig.configs.resolver.placeholder.strategies.EnvSpecificResolveStrategy.EnvProperty;
import io.microconfig.configs.resolver.placeholder.strategies.GeneralPropertiesResolveStrategy;
import io.microconfig.configs.resolver.placeholder.strategies.GeneralPropertiesResolveStrategy.GeneralProperty;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class FolderProperty implements GeneralProperty {
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