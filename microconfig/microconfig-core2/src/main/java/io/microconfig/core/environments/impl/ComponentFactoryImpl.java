package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Components;
import io.microconfig.core.properties.ComponentPropertiesFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypeRepository configTypeRepository;
    private final ComponentPropertiesFactory componentPropertiesFactory;

    @Override
    public Component createComponent(String componentName, String componentType, String environment) {
        return new ComponentImpl(configTypeRepository, componentPropertiesFactory, componentName, componentType, environment);
    }

    @Override
    public Components toComponents(List<Component> components) {
        return new ComponentsImpl(components);
    }
}