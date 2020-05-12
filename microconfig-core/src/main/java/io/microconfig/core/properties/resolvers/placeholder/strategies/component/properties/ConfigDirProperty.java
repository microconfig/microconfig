package io.microconfig.core.properties.resolvers.placeholder.strategies.component.properties;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.repository.ComponentGraph;
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty;
import io.microconfig.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@RequiredArgsConstructor
public class ConfigDirProperty implements ComponentProperty {
    private final ComponentGraph componentGraph;
    private final EnvironmentRepository environmentRepository;

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> resolveFor(String componentName, String environment) {
        Optional<String> dir = findDirBy(componentName);
        if (dir.isPresent()) return dir;

        return findDirBy(originalNameOf(componentName, environment));
    }

    private Optional<String> findDirBy(String component) {
        return componentGraph.getFolderOf(component)
                .map(File::getAbsolutePath)
                .map(StringUtils::unixLikePath);
    }

    private String originalNameOf(String component, String env) {
        return environmentRepository.getOrCreateByName(env)
                .findComponentWithName(component)
                .getOriginalName();
    }
}