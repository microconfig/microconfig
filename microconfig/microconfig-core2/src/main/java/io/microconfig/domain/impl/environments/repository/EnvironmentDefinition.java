package io.microconfig.domain.impl.environments.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Optional.empty;

@Getter
@RequiredArgsConstructor
public class EnvironmentDefinition {
    private final String name;
    private final String ip;
    private final Integer portOffset;
    private final EnvInclude envInclude;
    private final List<ComponentGroupDefinition> groups;

    private final File source;

    public EnvironmentDefinition withIncludedGroups(List<ComponentGroupDefinition> includedGroups) {
        return new EnvironmentDefinition(
                name,
                includedGroups,
                ip,
                portOffset,
                empty(),
                source
        );
    }

    public EnvironmentDefinition processInclude(EnvironmentProvider environmentProvider) {
        return include.map(env -> env.includeTo(this, environmentProvider))
                .orElse(this);
    }

    public EnvironmentDefinition verifyUniqueComponentNames() {
        Set<String> unique = new HashSet<>();
        groups.stream()
                .map(ComponentGroupDefinition::getComponents)
                .flatMap(Collection::stream)
                .filter(c -> !unique.add(c.getName()))
                .findFirst()
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Env [" + name + "] contains several definitions of [" + c.getName() + "] component");
                });

        return this;
    }
}