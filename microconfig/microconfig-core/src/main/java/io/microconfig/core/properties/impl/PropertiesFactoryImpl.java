package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.TypedProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.microconfig.utils.StreamUtils.forEach;

@RequiredArgsConstructor
public class PropertiesFactoryImpl implements PropertiesFactory {
    private final PropertiesRepository propertiesRepository;

    @Override
    public Properties getPropertiesOf(String componentName,
                                      String componentOriginalName,
                                      String environment,
                                      List<ConfigType> configTypes) {
        return new PropertiesImpl(
                forEach(configTypes, readConfigsFor(componentName, componentOriginalName, environment))
        );
    }

    private Function<ConfigType, TypedProperties> readConfigsFor(String componentName, String componentOriginalName, String environment) {
        return configType -> {
            Map<String, Property> properties = propertiesRepository.getPropertiesOf(componentOriginalName, environment, configType);
            return new TypedPropertiesImpl(configType, componentName, environment, properties);
        };
    }
}