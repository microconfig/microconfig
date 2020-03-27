package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.repository.ConfigFileParser.ConfigDefinition;
import io.microconfig.core.properties.repository.graph.ComponentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.core.properties.repository.graph.ConfigFileFilters.*;

@RequiredArgsConstructor
public class FilePropertiesRepository implements PropertiesRepository {
    private final ComponentGraph componentGraph;
    private final ConfigFileParser configFileParser;

    @Override
    public Map<String, Property> getPropertiesOf(String originalComponentName, String environment, ConfigType configType) {
        return new ComponentSource(originalComponentName, environment, configType).getProperties();
    }

    @RequiredArgsConstructor
    private class ComponentSource {
        @With
        private final String originalComponentName;
        @With
        private final String environment;

        private final ConfigType configType;
        private final Set<Include> processedIncludes;

        public ComponentSource(String originalComponentName, String environment, ConfigType configType) {
            this(originalComponentName, environment, configType, new LinkedHashSet<>());
        }

        public Map<String, Property> getProperties() {
            Map<String, Property> basicProperties = filter(defaultConfig(configType.getSourceExtensions()));
            Map<String, Property> envSharedProperties = filter(configForMultipleEnvironments(configType.getSourceExtensions(), environment));
            Map<String, Property> envSpecificProperties = filter(configForOneEnvironment(configType.getSourceExtensions(), environment));

            basicProperties.putAll(envSharedProperties);
            basicProperties.putAll(envSpecificProperties);
            return basicProperties;
        }

        private Map<String, Property> filter(Predicate<File> configFilter) {
            try {
                return collectPropertiesFrom(configDefinitionsFor(configFilter));
            } catch (ComponentNotFoundException e) {
                throw e.withParentComponent(originalComponentName);
            }
        }

        private Stream<ConfigDefinition> configDefinitionsFor(Predicate<File> filter) {
            return componentGraph.getConfigFilesFor(originalComponentName, filter)
                    .map(file -> configFileParser.parse(file, configType.getName(), environment));
        }

        private Map<String, Property> collectPropertiesFrom(Stream<ConfigDefinition> componentConfigs) {
            Map<String, Property> componentProperties = new LinkedHashMap<>();

            componentConfigs.forEach(component -> {
                componentProperties.putAll(component.getProperties());
                includedPropertiesFrom(component.getIncludes())
                        .forEach(componentProperties::putIfAbsent);
            });

            return componentProperties;
        }

        private Map<String, Property> includedPropertiesFrom(List<Include> includes) {
            return includes.stream()
                    .filter(processedIncludes::add)
                    .map(include -> includedComponent(include).getProperties())
                    .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }

        private ComponentSource includedComponent(Include include) {
            return withOriginalComponentName(include.getComponent())
                    .withEnvironment(include.getEnvironment());
        }
    }
}