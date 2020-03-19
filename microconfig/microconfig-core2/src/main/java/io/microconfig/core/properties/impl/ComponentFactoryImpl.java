package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.properties.Component;
import io.microconfig.core.properties.ComponentFactory;
import io.microconfig.core.properties.Components;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertiesRepository propertiesRepository;

    @Override
    public Component createComponent(String componentName, String componentType, String environment) {
        return new ComponentImpl(configTypeRepository, propertiesRepository, componentName, componentType, environment);
    }

    @Override
    public Components toComponents(List<Component> components) {
        return new ComponentsImpl(components);
    }
}