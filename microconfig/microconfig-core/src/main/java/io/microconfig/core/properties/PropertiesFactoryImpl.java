package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;
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

    @Override
    public Properties composite(List<Properties> properties) {
        return PropertiesImpl.composite(properties);
    }

    private Function<ConfigType, TypedProperties> readConfigsFor(String componentName, String componentOriginalName, String environment) {
        return configType -> {
            Map<String, Property> properties = propertiesRepository.getPropertiesOf(componentOriginalName, environment, configType);
            return new TypedPropertiesImpl(configType, componentName, environment, properties);
        };
    }
}