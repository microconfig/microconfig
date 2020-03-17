package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentFactory;
import io.microconfig.domain.Components;
import io.microconfig.domain.ConfigTypeRepository;
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