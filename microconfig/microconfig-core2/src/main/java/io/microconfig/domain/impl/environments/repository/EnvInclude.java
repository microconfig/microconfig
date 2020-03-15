package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microconfig.io.StreamUtils.toLinkedMap;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode
@RequiredArgsConstructor
public class EnvInclude {
    private final String env;
    private final Set<String> excludeGroups;

    public static EnvInclude empty() {
        return new EnvInclude("", emptySet());
    }

    public EnvironmentDefinition includeTo(EnvironmentDefinition includeTo, EnvironmentRepository environmentRepository) {
        Environment includeFrom = environmentRepository.withName(env);

        Map<String, ComponentGroup> groupToIncludeByName = collectGroupsToInclude(includeFrom)
                .stream()
                .map(includedGroup -> overrideIp(includedGroup, includeFrom, includeTo))
                .collect(toLinkedMap(ComponentGroup::getName, identity()));

        includeTo.getComponentGroups()
                .stream()
                .map(overriddenGroup -> override(groupToIncludeByName.get(overriddenGroup.getName()), overriddenGroup))
                .forEach(g -> groupToIncludeByName.put(g.getName(), g));

        return includeTo.withIncludedGroups(new ArrayList<>(groupToIncludeByName.values()));
    }

    private List<ComponentGroupDefinition> collectGroupsToInclude(Environment includeFrom) {
        return includeFrom.getComponentGroups()
                .stream()
                .filter(g -> !excludeGroups.contains(g.getName()))
                .collect(toList());
    }

    private ComponentGroupDefinition overrideIp(ComponentGroupDefinition includedGroup, Environment includedEnv, Environment destinationEnv) {
        if (destinationEnv.getIp().isPresent()) {
            return includedGroup.changeIp(destinationEnv.getIp().get());
        }

        if (!includedGroup.getIp().isPresent() && includedEnv.getIp().isPresent()) {
            return includedGroup.changeIp(includedEnv.getIp().get());
        }

        return includedGroup;
    }

    private ComponentGroupDefinition override(ComponentGroupDefinition includedGroup, ComponentGroup override) {
        return includedGroup == null ? override : includedGroup.override(override);
    }
}