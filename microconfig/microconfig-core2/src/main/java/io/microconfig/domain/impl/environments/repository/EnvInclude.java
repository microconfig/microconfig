package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.EnvironmentRepository;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.UnaryOperator;

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

    public EnvironmentDefinition includeTo(EnvironmentDefinition destinationEnv,
                                           EnvironmentRepository repository) {
        if (baseEnvironment.isEmpty()) return destinationEnv;

        EnvironmentDefinition baseEnv = (EnvironmentDefinition) repository.getWithName(baseEnvironment); //todo
        List<ComponentGroupDefinition> groupsToInclude = forEach(groupsToIncludeFrom(baseEnv), assignIpOf(destinationEnv));

        Map<String, ComponentGroupDefinition> groupToIncludeByName = groupsToInclude.stream()
                .collect(toLinkedMap(ComponentGroupDefinition::getName, identity()));

        destinationEnv.getGroups()
                .stream()
                .map(overriddenGroup -> override(groupToIncludeByName.get(overriddenGroup.getName()), overriddenGroup))
                .forEach(g -> groupToIncludeByName.put(g.getName(), g));

        return assignNewGroupsTo(destinationEnv, groupToIncludeByName.values());
    }

    private List<ComponentGroupDefinition> groupsToIncludeFrom(EnvironmentDefinition env) {
        return filter(env.getGroups(), g -> !excludedGroups.contains(g.getName()));
    }

    private UnaryOperator<ComponentGroupDefinition> assignIpOf(EnvironmentDefinition destinationEnv) {
        return group -> destinationEnv.getIp() == null ? group : group.withIp(destinationEnv.getIp());
    }

    private ComponentGroupDefinition override(ComponentGroupDefinition includedGroup, ComponentGroupDefinition override) {
        return includedGroup == null ? override : includedGroup.overrideBy(override);
    }

    private EnvironmentDefinition assignNewGroupsTo(EnvironmentDefinition destinationEnv, Collection<ComponentGroupDefinition> groups) {
        return destinationEnv.withGroups(new ArrayList<>(groups)).withEnvInclude(EnvInclude.empty());
    }
}