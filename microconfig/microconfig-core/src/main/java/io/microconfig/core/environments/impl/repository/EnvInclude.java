package io.microconfig.core.environments.impl.repository;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import static io.microconfig.utils.StreamUtils.*;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;

@RequiredArgsConstructor
class EnvInclude {
    private static final EnvInclude empty = new EnvInclude("", emptySet());

    private final String baseEnvironment;
    private final Set<String> excludedGroups;

    public static EnvInclude empty() {
        return empty;
    }

    public EnvironmentDefinition includeTo(EnvironmentDefinition destinationEnv, Function<String, EnvironmentDefinition> repository) {
        val baseGroupByName = findBaseGroupsUsing(repository, destinationEnv);
        forEach(destinationEnv.getGroups(), overrideBaseGroupIn(baseGroupByName).andThen(putOverriddenGroupTo(baseGroupByName)));
        return assignGroupsTo(destinationEnv, baseGroupByName.values());
    }

    private Map<String, ComponentGroupDefinition> findBaseGroupsUsing(Function<String, EnvironmentDefinition> repository, EnvironmentDefinition destinationEnv) {
        EnvironmentDefinition baseEnv = repository.apply(baseEnvironment);
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

    private Function<ComponentGroupDefinition, ComponentGroupDefinition> putOverriddenGroupTo(Map<String, ComponentGroupDefinition> baseGroupByName) {
        return overriddenGroup -> baseGroupByName.put(overriddenGroup.getName(), overriddenGroup);
    }

    public boolean isEmpty() {
        return baseEnvironment.isEmpty();
    }
}