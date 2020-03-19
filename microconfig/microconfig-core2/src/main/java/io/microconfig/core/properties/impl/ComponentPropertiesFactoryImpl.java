package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.ComponentProperties;
import io.microconfig.core.properties.ComponentPropertiesFactory;
import io.microconfig.core.properties.CompositeComponentProperties;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.properties.impl.CompositeComponentPropertiesImpl.resultsOf;
import static io.microconfig.utils.StreamUtils.forEach;

@RequiredArgsConstructor
public class ComponentPropertiesFactoryImpl implements ComponentPropertiesFactory {
    private final PropertiesRepository propertiesRepository;

    @Override
    public CompositeComponentProperties getComponentProperties(String componentType, String environment, List<ConfigType> configTypes) {
        return resultsOf(forEach(configTypes, configType -> readConfigs(componentType, environment, configType)));
    }

    private ComponentProperties readConfigs(String component, String environment, ConfigType configType) {
        List<Property> properties = propertiesRepository.getProperties(component, environment, configType);
        return new ComponentPropertiesImpl(component, environment, configType, properties);
    }
}
