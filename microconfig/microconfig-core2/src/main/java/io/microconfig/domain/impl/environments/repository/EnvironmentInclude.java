package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.EnvironmentRepository;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microconfig.io.StreamUtils.filter;
import static io.microconfig.io.StreamUtils.toLinkedMap;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;

@EqualsAndHashCode
@RequiredArgsConstructor
public class EnvironmentInclude {
    private static final EnvironmentInclude empty = new EnvironmentInclude("", emptySet());

    private final String environment;
    private final Set<String> excludeGroups;

    public static EnvironmentInclude empty() {
        return empty;
    }

    public EnvironmentDefinition includeTo(EnvironmentDefinition includeTo,
                                           EnvironmentRepository environmentRepository) {
        if (environment.isEmpty()) return includeTo;

        //todo
        EnvironmentDefinition includeFrom = (EnvironmentDefinition) environmentRepository.withName(environment);

        Map<String, ComponentGroupDefinition> groupToIncludeByName = collectGroupsToInclude(includeFrom)
                .stream()
                .map(includedGroup -> overrideIp(includedGroup, includeFrom, includeTo))
                .collect(toLinkedMap(ComponentGroupDefinition::getName, identity()));

        includeTo.getGroups()
                .stream()
                .map(overriddenGroup -> override(groupToIncludeByName.get(overriddenGroup.getName()), overriddenGroup))
                .forEach(g -> groupToIncludeByName.put(g.getName(), g));

        return includeTo.withIncludedGroups(new ArrayList<>(groupToIncludeByName.values()));
    }

    private List<ComponentGroupDefinition> collectGroupsToInclude(EnvironmentDefinition includeFrom) {
        return filter(includeFrom.getGroups(), g -> !excludeGroups.contains(g.getName()));
    }

    private ComponentGroupDefinition overrideIp(ComponentGroupDefinition includedGroup, EnvironmentDefinition includedEnv, EnvironmentDefinition destinationEnv) {
        if (destinationEnv.getIp() != null) {
            return includedGroup.withIp(destinationEnv.getIp());
        }

        if (includedGroup.getIp() == null && includedEnv.getIp() != null) {
            return includedGroup.withIp(includedEnv.getIp());
        }

        return includedGroup;
    }

    private ComponentGroupDefinition override(ComponentGroupDefinition includedGroup, ComponentGroupDefinition override) {
        return includedGroup == null ? override : includedGroup.overrideBy(override);
    }
}