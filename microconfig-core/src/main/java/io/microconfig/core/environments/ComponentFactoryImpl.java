package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.properties.PropertiesFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertiesFactory propertiesFactory;

    @Override
    public Component createComponent(String componentName,
                                     String componentOriginalName,
                                     String environment) {
        return new ComponentImpl(configTypeRepository, propertiesFactory, componentName, componentOriginalName, environment);
    }
}