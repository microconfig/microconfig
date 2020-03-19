package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.CompositeProperties;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;

import static io.microconfig.core.properties.impl.CompositePropertiesImpl.composite;
import static io.microconfig.utils.StreamUtils.forEach;

@RequiredArgsConstructor
public class PropertiesFactoryImpl implements PropertiesFactory {
    private final PropertiesRepository propertiesRepository;

    @Override
    public CompositeProperties getPropertiesOf(String component,
                                               String environment,
                                               List<ConfigType> configTypes) {
        return composite(
                forEach(configTypes, readConfigsFor(component, environment))
        );
    }

    private Function<ConfigType, Properties> readConfigsFor(String component, String environment) {
        return configType -> {
            List<Property> properties = propertiesRepository.getProperties(component, environment, configType);
            return new PropertiesImpl(component, environment, configType, properties);
        };
    }
}