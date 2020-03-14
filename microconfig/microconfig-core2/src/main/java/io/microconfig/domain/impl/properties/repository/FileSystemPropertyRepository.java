package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertyRepository;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.microconfig.io.fsgraph.ConfigFileFilters.*;

@RequiredArgsConstructor
public class FileSystemPropertyRepository implements PropertyRepository {
    private final FileSystemGraph fsGraph;
    private final ConfigParser configParser;

    @Override
    public List<Property> getProperties(String __, String componentType, String environment, ConfigType configType) {
        return new ArrayList<>(
                collectProperties(componentType, environment, configType.getSourceExtensions(), new LinkedHashSet<>()).values()
        );
    }

    private Map<String, Property> collectProperties(String componentType, String env, Set<String> configExtensions, Set<Include> processedIncludes) {
        Function<Predicate<File>, Map<String, Property>> collectProperties = filter -> collectProperties(filter, componentType, env, configExtensions, processedIncludes);

        Map<String, Property> basicProperties = collectProperties.apply(defaultConfig(configExtensions));
        Map<String, Property> envSharedProperties = collectProperties.apply(configForMultipleEnvironments(configExtensions, env));
        Map<String, Property> envSpecificProperties = collectProperties.apply(configForOneEnvironment(configExtensions, env));

        basicProperties.putAll(envSharedProperties);
        basicProperties.putAll(envSpecificProperties);
        return basicProperties;
    }

    private Map<String, Property> collectProperties(Predicate<File> filter, String componentType, String env, Set<String> configExtensions, Set<Include> processedIncludes) {
        Map<String, Property> propertyByKey = new TreeMap<>();

        fsGraph.getConfigFilesFor(componentType, filter)
                .map(file -> configParser.parse(file, env))
                .forEach(c -> processComponent(c, propertyByKey, configExtensions, processedIncludes));

        return propertyByKey;
    }

    private void processComponent(ParsedConfig parsedConfig, Map<String, Property> destination, Set<String> configExtensions, Set<Include> processedIncludes) {
        Map<String, Property> included = processIncludes(parsedConfig.getIncludes(), configExtensions, processedIncludes);
        Map<String, Property> original = parsedConfig.getPropertiesAsMas();

        destination.putAll(included);
        destination.putAll(original);
    }

    private Map<String, Property> processIncludes(List<Include> includes, Set<String> configExtensions, Set<Include> processedIncludes) {
        Map<String, Property> result = new HashMap<>();

        for (Include include : includes) {
            if (!processedIncludes.add(include)) continue;

            Map<String, Property> included = collectProperties(include.getComponentType(), include.getEnvironment(), configExtensions, processedIncludes);
            result.putAll(included);
        }

        return result;
    }
}
