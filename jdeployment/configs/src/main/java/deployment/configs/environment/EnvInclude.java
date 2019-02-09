package deployment.configs.environment;

import java.util.*;

import static deployment.util.StreamUtils.toLinkedMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

public class EnvInclude {
    private final String env;
    private final Set<String> excludeGroups;

    public EnvInclude(String env, Set<String> excludeGroups) {
        this.env = requireNonNull(env);
        this.excludeGroups = unmodifiableSet(requireNonNull(excludeGroups));
    }

    public Environment includeTo(Environment destinationEnv, EnvironmentProvider environmentProvider) {
        Environment baseEnv = environmentProvider.getByName(env);

        Map<String, ComponentGroup> groupToIncludeByName = getGroupsToInclude(baseEnv.getComponentGroups())
                .stream()
                .map(g -> overrideIpFromEnv(g, baseEnv, destinationEnv))
                .collect(toLinkedMap(ComponentGroup::getName, identity()));

        destinationEnv.getComponentGroups().stream()
                .map(g -> overrideComponentGroup(g, groupToIncludeByName.get(g.getName())))
                .forEach(g -> groupToIncludeByName.put(g.getName(), g));

        return new Environment(destinationEnv.getName(), new ArrayList<>(groupToIncludeByName.values()),
                destinationEnv.getIp(), destinationEnv.getPortOffset(), Optional.empty());
    }

    private ComponentGroup overrideIpFromEnv(ComponentGroup baseGroup, Environment baseEnv, Environment destinationEnv) {
        if (baseEnv.getIp().isPresent() && !baseGroup.getIp().isPresent()) {
            baseGroup = baseGroup.changeIp(baseEnv.getIp().get());
        }
        if (destinationEnv.getIp().isPresent()) {
            baseGroup = baseGroup.changeIp(destinationEnv.getIp().get());
        }
        return baseGroup;
    }

    private ComponentGroup overrideComponentGroup(ComponentGroup overriddenGroup, ComponentGroup baseGroup) {
        return baseGroup == null ? overriddenGroup : overrideIpAndComponents(overriddenGroup, baseGroup);
    }

    private ComponentGroup overrideIpAndComponents(ComponentGroup overridenGroup, ComponentGroup baseGroup) {
        if (overridenGroup.getIp().isPresent()) {
            baseGroup = baseGroup.changeIp(overridenGroup.getIp().get());
        }

        if (!overridenGroup.getComponents().isEmpty()) {
            baseGroup = baseGroup.changeComponents(overridenGroup.getComponents());
        }

        if (!overridenGroup.getExcludedComponents().isEmpty()) {
            baseGroup = baseGroup.excludeComponents(overridenGroup.getExcludedComponents());
        }

        if (!overridenGroup.getAppendedComponents().isEmpty()) {
            baseGroup = baseGroup.appendComponents(overridenGroup.getAppendedComponents());
        }

        return baseGroup;
    }

    private List<ComponentGroup> getGroupsToInclude(List<ComponentGroup> componentGroups) {
        return componentGroups.stream()
                .filter(g -> !excludeGroups.contains(g.getName()))
                .collect(toList());
    }
}
