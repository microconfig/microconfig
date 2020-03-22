package io.microconfig.core.resolvers.placeholder.strategies.component.properties;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.impl.repository.ComponentGraph;
import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentProperty;
import io.microconfig.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

import static java.util.Optional.empty;

@RequiredArgsConstructor
public class ConfigDirProperty implements ComponentProperty {
    private final ComponentGraph componentGraph;
    private final EnvironmentRepository environmentRepository;

    @Override
    public String key() {
        return "configDir";
    }

    @Override
    public Optional<String> resolveFor(String component, String environment) {
        Optional<String> result = tryFind(component);
        if (result.isPresent()) return result;

        String originalName = getOriginalNameOf(component, environment);
        if (originalName.equals(component)) return empty();
        return tryFind(originalName);
    }

    private Optional<String> tryFind(String component) {
        return componentGraph.getFolderOf(component)
                .map(File::getAbsolutePath)
                .map(StringUtils::unixLikePath);
    }

    private String getOriginalNameOf(String component, String env) {
        return environmentRepository.getOrCreateByName(env)
                .getOrCreateComponentWithName(component)
                .getOriginalName();
    }
}