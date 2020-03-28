package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ConfigIo;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.*;

import static java.util.Collections.emptyMap;

@RequiredArgsConstructor
public class FilePropertiesRepository implements PropertiesRepository {
    private final ConfigFileRepository configFileRepository;
    private final ConfigIo configIo;

    @Override
    public Map<String, Property> getPropertiesOf(String originalComponentName, String environment, ConfigType configType) {
        return new ComponentSource(originalComponentName, environment, configType, new LinkedHashSet<>()).getProperties();
    }

    @RequiredArgsConstructor
    private class ComponentSource {
        @With
        private final String originalComponentName;
        @With
        private final String environment;
        private final ConfigType configType;

        private final Set<Include> processedIncludes;

        public Map<String, Property> getProperties() {
            return collectPropertiesFrom(configFiles());
        }

        private List<ConfigFile> configFiles() {
            try {
                return configFileRepository.getConfigFilesFor(originalComponentName, environment, configType);
            } catch (ComponentNotFoundException e) {
                throw e.withParentComponent(originalComponentName); //todo test
            }
        }

        private Map<String, Property> collectPropertiesFrom(List<ConfigFile> componentConfigs) {
            return componentConfigs.stream()
                    .map(cf -> cf.parseUsing(configIo))
                    .map(this::getComponentProperties)
                    .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }

        private Map<String, Property> getComponentProperties(ConfigDefinition component) {
            return component.getBaseAndIncludedProperties(this::includeResolver);
        }

        private Map<String, Property> includeResolver(Include include) {
            return processedIncludes.add(include) ? componentFrom(include).getProperties() : emptyMap();
        }

        private ComponentSource componentFrom(Include include) {
            return withOriginalComponentName(include.getComponent())
                    .withEnvironment(include.getEnvironment());
        }
    }
}