package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.parser.Include;
import io.microconfig.domain.impl.properties.parser.ParsedComponent;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.microconfig.domain.impl.properties.CompositeComponentConfigurationImpl.resultsOf;
import static io.microconfig.io.StreamUtils.forEach;
import static io.microconfig.io.fsgraph.ConfigFileFilters.*;

@RequiredArgsConstructor
public class FileSystemComponent implements Component {
    private final ConfigTypes types;
    private final FileSystemGraph fsGraph;

    @Getter
    private final String name;
    @Getter
    private final String environment;

    @Override
    public CompositeComponentConfiguration getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(types.getTypes());
        return resultsOf(forEach(filteredTypes, this::readConfigs));
    }

    private ComponentConfiguration readConfigs(ConfigType type) {
        return new ComponentConfigurationImpl(name, environment, type, readPropertiesWith(type));
    }

    private List<Property> readPropertiesWith(ConfigType type) {
        return Collections.emptyList();
    }

    private class PropertyProvider {
        public Map<String, Property> readProperties() {
            return collectProperties(new LinkedHashSet<>());
        }

        private Map<String, Property> collectProperties(Set<Include> processedIncludes) {
            Function<Predicate<File>, Map<String, Property>> collectProperties = filter -> collectProperties(filter, processedIncludes);

            Map<String, Property> basicProperties = collectProperties.apply(defaultConfig(configExtensions));
            Map<String, Property> envSharedProperties = collectProperties.apply(configForMultipleEnvironments(configExtensions, env));
            Map<String, Property> envSpecificProperties = collectProperties.apply(configForOneEnvironment(configExtensions, env));

            basicProperties.putAll(envSharedProperties);
            basicProperties.putAll(envSpecificProperties);
            return basicProperties;
        }

        private Map<String, Property> collectProperties(Predicate<File> filter, Set<Include> processedIncludes) {
            Map<String, Property> propertyByKey = new HashMap<>();

            fsGraph.getConfigFilesFor(component.getType(), filter)
                    .map(file -> componentParser.parse(file, env))
                    .forEach(c -> processComponent(c, propertyByKey, processedIncludes));

            return propertyByKey;
        }

        private void processComponent(ParsedComponent parsedComponent, Map<String, Property> destination, Set<Include> processedIncludes) {
            Map<String, Property> included = processIncludes(parsedComponent.getIncludes(), processedIncludes);
            Map<String, Property> original = parsedComponent.getPropertiesAsMas();

            destination.putAll(included);
            destination.putAll(original);
        }

        private Map<String, Property> processIncludes(List<Include> includes, Set<Include> processedIncludes) {
            Map<String, Property> result = new HashMap<>();

            for (Include include : includes) {
                if (!processedIncludes.add(include)) continue;

                Map<String, Property> included = collectProperties(byType(include.getComponent()), include.getEnv(), processedIncludes);
                result.putAll(included);
            }

            return result;
        }
    }
}