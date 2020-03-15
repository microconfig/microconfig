package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertyRepository;
import io.microconfig.io.fsgraph.ComponentDoesNotExistException;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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
                    return collectPropertiesFor(componentType, filter, env);
                } catch (ComponentDoesNotExistException e) {
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

        private Map<String, Property> collectPropertiesFor(String componentType, Predicate<File> filter, String env) {
            Map<String, Property> propertyByKey = new HashMap<>();

            fsGraph.getConfigFilesFor(componentType, filter)
                    .map(file -> configParser.parse(file, env))
                    .forEach(c -> processComponent(c, propertyByKey));

            return propertyByKey;
        }

        private void processComponent(ConfigDefinition configDefinition, Map<String, Property> destination) {
            Map<String, Property> included = processIncludes(configDefinition.getIncludes());
            Map<String, Property> original = configDefinition.getPropertiesAsMas();

            destination.putAll(included);
            destination.putAll(original);
        }

        private Map<String, Property> processIncludes(List<Include> includes) {
            Map<String, Property> result = new HashMap<>();

            includes.stream()
                    .filter(processedIncludes::add)
                    .map(include -> collectPropertiesFor(include.getComponentType(), include.getEnvironment()))
                    .forEach(result::putAll);


            return result;
        }

    }
}
