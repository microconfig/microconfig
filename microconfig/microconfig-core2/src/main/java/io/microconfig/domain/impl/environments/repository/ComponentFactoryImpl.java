package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigTypeRepository;
import io.microconfig.domain.impl.environments.ComponentFactory;
import io.microconfig.domain.impl.properties.ComponentImpl;
import io.microconfig.domain.impl.properties.PropertiesRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertiesRepository propertiesRepository;

    @Override
    public Component createComponent(String componentName, String componentType, String environment) {
        return new ComponentImpl(configTypeRepository, propertiesRepository, componentName, componentType, environment);
    }
}