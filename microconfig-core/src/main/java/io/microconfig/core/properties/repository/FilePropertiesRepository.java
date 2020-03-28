package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ConfigIo;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class FilePropertiesRepository implements PropertiesRepository {
    private final ConfigFileRepository configFileRepository;
    private final ConfigIo configIo;

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
            try {
                return collectPropertiesFrom(configFiles());
            } catch (ComponentNotFoundException e) {
                throw e.withParentComponent(originalComponentName);
            }
        }

        private Stream<ConfigFile> configFiles() {
            return configFileRepository.getConfigFilesFor(originalComponentName, environment, configType);
        }

        private Map<String, Property> collectPropertiesFrom(Stream<ConfigFile> componentConfigs) {
            Map<String, Property> componentProperties = new LinkedHashMap<>();

            componentConfigs.map(cf -> cf.parseUsing(configIo))
                    .forEach(component -> {
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