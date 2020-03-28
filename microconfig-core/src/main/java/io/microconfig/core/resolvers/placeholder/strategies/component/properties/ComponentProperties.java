package io.microconfig.core.resolvers.placeholder.strategies.component.properties;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.repository.ConfigFileRepository;
import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentProperty;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ComponentProperties {
    private final ConfigFileRepository configFileRepository;
    private final EnvironmentRepository environmentRepository;
    private final File rootDir;
    private final File destinationComponentDir;

    public Map<String, ComponentProperty> get() {
        return of(
                new NameProperty(),
                new ConfigDirProperty(configFileRepository, environmentRepository),
                new ResultDirProperty(destinationComponentDir),
                new ConfigRootDirProperty(rootDir)
        ).collect(toMap(ComponentProperty::key, identity()));
    }
}