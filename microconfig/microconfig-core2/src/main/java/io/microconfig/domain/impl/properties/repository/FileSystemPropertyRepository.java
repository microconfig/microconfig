package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertyRepository;
import io.microconfig.io.fsgraph.ComponentNotFoundException;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.io.fsgraph.ConfigFileFilters.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class FileSystemPropertyRepository implements PropertyRepository {
    private final FileSystemGraph fsGraph;
    private final ConfigParser configParser;

    @Override
    public List<Property> getProperties(String componentType, String environment, ConfigType configType) {
        return new CollectorContext(configType.getSourceExtensions())
                .collectPropertiesFor(componentType, environment)
                .values().stream()
                .sorted(comparing(Property::getKey))
                .collect(toList());
    }

    @RequiredArgsConstructor
    private class CollectorContext {
        private final Set<String> configExtensions;
        private final Set<Include> processedIncludes = new LinkedHashSet<>();

        public Map<String, Property> collectPropertiesFor(String componentType, String env) {
            Function<Predicate<File>, Map<String, Property>> collectProperties = filter -> {
                try {
                    return collectPropertiesFrom(configDefinitionsFor(componentType, filter, env));
                } catch (ComponentNotFoundException e) {
                    throw e.withComponentParent(componentType);
                }
            };

            Map<String, Property> basicProperties = collectProperties.apply(defaultConfig(configExtensions));
            Map<String, Property> envSharedProperties = collectProperties.apply(configForMultipleEnvironments(configExtensions, env));
            Map<String, Property> envSpecificProperties = collectProperties.apply(configForOneEnvironment(configExtensions, env));

            basicProperties.putAll(envSharedProperties);
            basicProperties.putAll(envSpecificProperties);
            return basicProperties;
        }

        private Stream<ConfigDefinition> configDefinitionsFor(String componentType, Predicate<File> filter, String env) {
            return fsGraph.getConfigFilesFor(componentType, filter)
                    .map(file -> configParser.parse(file, env));
        }

        private Map<String, Property> collectPropertiesFrom(Stream<ConfigDefinition> componentConfigs) {
            Map<String, Property> componentProperties = new HashMap<>();

            componentConfigs.forEach(component -> {
                componentProperties.putAll(collectIncludedProperties(component.getIncludes()));
                componentProperties.putAll(component.getProperties());
            });

            return componentProperties;
        }

        private Map<String, Property> collectIncludedProperties(List<Include> includes) {
            return includes.stream()
                    .filter(processedIncludes::add)
                    .map(include -> collectPropertiesFor(include.getComponentType(), include.getEnvironment()))
                    .reduce(new HashMap<>(), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }
    }
}
