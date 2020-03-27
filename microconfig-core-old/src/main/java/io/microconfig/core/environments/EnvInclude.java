package io.microconfig.core.environments;

import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode
public class EnvInclude {
    private final String env;
    private final Set<String> excludeGroups;

    public EnvInclude(String env, Set<String> excludeGroups) {
        this.env = requireNonNull(env);
        this.excludeGroups = unmodifiableSet(requireNonNull(excludeGroups));
    }

    public Environment includeTo(Environment includeTo, EnvironmentProvider environmentProvider) {
        Environment includeFrom = environmentProvider.getByName(env);

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

    private List<ComponentGroup> collectGroupsToInclude(Environment includeFrom) {
        return includeFrom.getComponentGroups()
                .stream()
                .filter(g -> !excludeGroups.contains(g.getName()))
                .collect(toList());
    }

    private ComponentGroup overrideIp(ComponentGroup includedGroup, Environment includedEnv, Environment destinationEnv) {
        if (destinationEnv.getIp().isPresent()) {
            return includedGroup.changeIp(destinationEnv.getIp().get());
        }

        if (!includedGroup.getIp().isPresent() && includedEnv.getIp().isPresent()) {
            return includedGroup.changeIp(includedEnv.getIp().get());
        }

        return includedGroup;
    }

    private ComponentGroup override(ComponentGroup includedGroup, ComponentGroup override) {
        return includedGroup == null ? override : includedGroup.override(override);
    }
}