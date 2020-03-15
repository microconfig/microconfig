package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.domain.impl.environments.ComponentFactory;
import io.microconfig.domain.impl.environments.EnvironmentImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@With
@RequiredArgsConstructor
public class EnvironmentDefinition {
    private final String name;
    @Getter
    private final String ip;
    private final int portOffset;
    private final EnvInclude envInclude;
    @Getter
    private final List<ComponentGroupDefinition> groups;

    private final File source;

    public EnvironmentDefinition processInclude(EnvironmentRepository environmentProvider) {
        return envInclude.includeTo(this, environmentProvider);
    }

    public EnvironmentDefinition checkComponentNamesAreUnique() {
        List<String> notUniqueComponents = groups.stream()
                .map(ComponentGroupDefinition::getComponents)
                .flatMap(List::stream)
                .collect(groupingBy(ComponentDefinition::getName))
                .entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(toList());

        if (!notUniqueComponents.isEmpty()) {
            throw new IllegalStateException("Env '" + name + "' contains several declarations of: " + notUniqueComponents);
        }

        return this;
    }

    public Environment toEnvironment(ComponentFactory componentFactory) {
        return new EnvironmentImpl(
                name,
                forEach(groups, g -> g.toGroup(componentFactory, name)),
                componentFactory
        );
    }
}