package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.EnvironmentRepository;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import static io.microconfig.utils.StreamUtils.*;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;

@EqualsAndHashCode
@RequiredArgsConstructor
public class EnvInclude {
    private static final EnvInclude empty = new EnvInclude("", emptySet());

    private final String baseEnvironment;
    private final Set<String> excludedGroups;

    public static EnvInclude empty() {
        return empty;
    }

    public EnvironmentDefinition includeTo(EnvironmentDefinition destinationEnv, EnvironmentRepository repository) {
        val baseGroupByName = findBaseGroupsUsing(repository, destinationEnv);
        forEach(destinationEnv.getGroups(), overrideBaseGroupIn(baseGroupByName).andThen(g -> baseGroupByName.put(g.getName(), g)));
        return assignGroupsTo(destinationEnv, baseGroupByName.values());
    }

    private Map<String, ComponentGroupDefinition> findBaseGroupsUsing(EnvironmentRepository repository, EnvironmentDefinition destinationEnv) {
        EnvironmentDefinition baseEnv = (EnvironmentDefinition) repository.getByName(baseEnvironment);
        return forEach(notExcludedGroupsFrom(baseEnv), assignIpOf(destinationEnv), resultsToMap());
    }

    private List<ComponentGroupDefinition> notExcludedGroupsFrom(EnvironmentDefinition baseEnv) {
        return filter(baseEnv.getGroups(), group -> !excludedGroups.contains(group.getName()));
    }

    private UnaryOperator<ComponentGroupDefinition> assignIpOf(EnvironmentDefinition destinationEnv) {
        return group -> destinationEnv.getIp() == null ? group : group.withIp(destinationEnv.getIp());
    }

    private UnaryOperator<ComponentGroupDefinition> overrideBaseGroupIn(Map<String, ComponentGroupDefinition> baseGroupByName) {
        return destinationGroup -> {
            ComponentGroupDefinition baseGroup = baseGroupByName.get(destinationGroup.getName());
            return baseGroup == null ? destinationGroup : baseGroup.overrideBy(destinationGroup);
        };
    }

    private EnvironmentDefinition assignGroupsTo(EnvironmentDefinition destinationEnv, Collection<ComponentGroupDefinition> groups) {
        return destinationEnv.withGroups(new ArrayList<>(groups))
                .withEnvInclude(EnvInclude.empty());
    }

    private Collector<ComponentGroupDefinition, ?, Map<String, ComponentGroupDefinition>> resultsToMap() {
        return toLinkedMap(ComponentGroupDefinition::getName, identity());
    }

    public boolean isEmpty() {
        return this == empty();
    }
}