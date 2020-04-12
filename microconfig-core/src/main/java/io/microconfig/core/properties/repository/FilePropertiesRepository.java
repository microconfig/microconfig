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
    private final ComponentGraph componentGraph;
    private final ConfigIo configIo;

    @Override
    public Map<String, Property> getPropertiesOf(String originalComponentName, String environment, ConfigType configType) {
        return new OriginalComponent(originalComponentName, environment, configType, new LinkedHashSet<>()).getProperties();
    }

    @RequiredArgsConstructor
    private class OriginalComponent {
        @With
        private final String component;
        @With
        private final String environment;
        private final ConfigType configType;

        private final Set<Include> processedIncludes;

        public Map<String, Property> getProperties() {
            return readAndParse(configFiles());
        }

        private List<ConfigFile> configFiles() {
            return componentGraph.getConfigFilesOf(component, environment, configType);
        }

        private Map<String, Property> readAndParse(List<ConfigFile> componentConfigFiles) {
            return componentConfigFiles.stream()
                    .map(this::parse)
                    .reduce(new LinkedHashMap<>(), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }

        private Map<String, Property> parse(ConfigFile configFile) {
            return configFile.parseUsing(configIo)
                    .getBaseAndIncludedProperties(this::includeResolver);
        }

        private Map<String, Property> includeResolver(Include include) {
            try {
                return processedIncludes.add(include) ? componentFrom(include).getProperties() : emptyMap();
            } catch (ComponentNotFoundException e) {
                throw e.withParentComponent(component);
            }
        }

        private OriginalComponent componentFrom(Include include) {
            return withComponent(include.getComponent())
                    .withEnvironment(include.getEnvironment());
        }
    }
}