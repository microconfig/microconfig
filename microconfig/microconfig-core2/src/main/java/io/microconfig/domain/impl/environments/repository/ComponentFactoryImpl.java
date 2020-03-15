package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigTypeRepository;
import io.microconfig.domain.impl.environments.ComponentFactory;
import io.microconfig.domain.impl.properties.ComponentImpl;
import io.microconfig.domain.impl.properties.PropertyRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertyRepository propertyRepository;

    @Override
    public Component createComponent(String componentName, String componentType, String environment) {
        return new ComponentImpl(
                configTypeRepository,
                propertyRepository,
                componentName,
                componentType,
                environment
        );
    }
}