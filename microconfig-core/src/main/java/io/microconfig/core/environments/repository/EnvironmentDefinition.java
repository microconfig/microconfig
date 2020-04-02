package io.microconfig.core.environments.repository;

import io.microconfig.core.environments.ComponentFactory;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentImpl;
import io.microconfig.core.properties.PropertiesFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.stream.Collectors.*;

@With
@RequiredArgsConstructor
class EnvironmentDefinition {
    private final String name;
    @Getter
    private final String ip;
    private final int portOffset;
    private final EnvInclude envInclude;
    @Getter
    private final List<ComponentGroupDefinition> groups;

    public EnvironmentDefinition processIncludeUsing(Function<String, EnvironmentDefinition> environmentRepository) {
        return envInclude.isEmpty() ? this : envInclude.includeTo(this, environmentRepository);
    }

    public EnvironmentDefinition checkComponentNamesAreUnique() {
        List<String> notUniqueComponents = groups.stream()
                .map(ComponentGroupDefinition::getComponents)
                .flatMap(List::stream)
                .collect(groupingBy(ComponentDefinition::getName, counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Entry::getKey)
                .collect(toList());

        if (!notUniqueComponents.isEmpty()) {
            throw new IllegalStateException("Environment '" + name + "' contains several declarations of: " + notUniqueComponents);
        }

        return this;
    }

    public Environment toEnvironment(ComponentFactory componentFactory, PropertiesFactory propertiesFactory) {
        return new EnvironmentImpl(
                name,
                portOffset,
                forEach(groups, g -> g.toGroup(componentFactory, propertiesFactory, name)),
                componentFactory,
                propertiesFactory
        );
    }
}