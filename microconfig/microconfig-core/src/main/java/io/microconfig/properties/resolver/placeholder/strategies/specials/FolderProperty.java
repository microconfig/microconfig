package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.files.provider.ComponentTree;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolverStrategy.SpecialProperty;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class FolderProperty implements SpecialProperty {
    private final ComponentTree componentTree;

    @Override
    public String key() {
        return "folder";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return componentTree.getFolder(component.getType())
                .map(File::getAbsolutePath);
    }
}
