package io.microconfig.core.environments.impl.repository;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.impl.ComponentImpl;
import io.microconfig.core.properties.PropertiesFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertiesFactory propertiesFactory;

    @Override
    public Component createComponent(String name, String type, String environment) {
        return new ComponentImpl(configTypeRepository, propertiesFactory, name, type, environment);
    }
}
