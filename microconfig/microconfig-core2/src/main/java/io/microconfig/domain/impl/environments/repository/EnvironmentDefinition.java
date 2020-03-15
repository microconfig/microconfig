package io.microconfig.domain.impl.environments.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Getter
@With
@RequiredArgsConstructor
public class EnvironmentDefinition {
    private final String name;
    private final String ip;
    private final Integer portOffset;
    private final EnvInclude envInclude;
    private final List<ComponentGroupDefinition> groups;

    private final File source;

    public EnvironmentDefinition withIncludedGroups(List<ComponentGroupDefinition> includedGroups) {
        return withGroups(includedGroups)
                .withEnvInclude(EnvInclude.empty());
    }

    public EnvironmentDefinition processInclude(EnvironmentProvider environmentProvider) {
        return envInclude.includeTo(this, environmentProvider))
    }

    public EnvironmentDefinition verifyUniqueComponentNames() {
        List<String> notUniqueComponents = groups.stream()
                .map(ComponentGroupDefinition::getComponents)
                .flatMap(List::stream)
                .collect(groupingBy(ComponentDefinition::getName))
                .entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(toList());

        if (!notUniqueComponents.isEmpty()) {
            throw new IllegalArgumentException("Env '" + name + "' contains several definitions of: " + notUniqueComponents);
        }

        return this;
    }
}