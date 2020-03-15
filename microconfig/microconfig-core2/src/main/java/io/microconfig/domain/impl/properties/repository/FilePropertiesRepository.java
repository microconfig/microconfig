package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertiesRepository;
import io.microconfig.domain.impl.properties.io.ConfigIo;
import io.microconfig.domain.impl.properties.repository.ConfigFile.ConfigDefinition;
import io.microconfig.domain.impl.properties.repository.graph.ComponentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.domain.impl.properties.repository.graph.ConfigFileFilters.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class FilePropertiesRepository implements PropertiesRepository {
    private final ComponentGraph componentGraph;
    private final ConfigIo ioService;

    @Override
    public List<Property> getProperties(String componentType, String environment, ConfigType configType) {
        return new ComponentSource(componentType, environment, configType)
                .getProperties()
                .values().stream()
                .sorted(comparing(Property::getKey))
                .collect(toList());
    }

    @RequiredArgsConstructor
    private class ComponentSource {
        @With
        private final String componentType;
        @With
        private final String environment;

        private final Set<String> configExtensions;
        private final Set<Include> processedIncludes;

        public ComponentSource(String componentType, String environment, ConfigType configType) {
            this(componentType, environment, configType.getSourceExtensions(), new LinkedHashSet<>());
        }

        public Map<String, Property> getProperties() {
            Map<String, Property> basicProperties = filter(defaultConfig(configExtensions));
            Map<String, Property> envSharedProperties = filter(configForMultipleEnvironments(configExtensions, environment));
            Map<String, Property> envSpecificProperties = filter(configForOneEnvironment(configExtensions, environment));

            basicProperties.putAll(envSharedProperties);
            basicProperties.putAll(envSpecificProperties);
            return basicProperties;
        }

        private Map<String, Property> filter(Predicate<File> configFilter) {
            try {
                return collectPropertiesFrom(configDefinitionsFor(configFilter));
            } catch (ComponentNotFoundException e) {
                throw e.withParentComponent(componentType);
            }
        }

        private Stream<ConfigDefinition> configDefinitionsFor(Predicate<File> filter) {
            return componentGraph.getConfigFilesFor(componentType, filter)
                    .map(file -> new ConfigFile(file, environment))
                    .map(original -> original.parseUsing(ioService));
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
                    .map(include -> includedComponent(include).getProperties())
                    .reduce(new HashMap<>(), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }

        private ComponentSource includedComponent(Include include) {
            return withComponentType(include.getComponentType())
                    .withEnvironment(include.getEnvironment());
        }
    }
}
