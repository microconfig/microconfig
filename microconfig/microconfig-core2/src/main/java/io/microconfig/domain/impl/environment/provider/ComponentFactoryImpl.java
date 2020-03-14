package io.microconfig.domain.impl.environment.provider;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigTypes;
import io.microconfig.domain.impl.environment.ComponentFactory;
import io.microconfig.domain.impl.properties.ComponentImpl;
import io.microconfig.domain.impl.properties.PropertyRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ComponentFactoryImpl implements ComponentFactory {
    private final ConfigTypes configTypes;
    private final PropertyRepository propertyRepository;

    @Override
    public Component createComponent(String componentAlias, String componentType, String environment) {
        return new ComponentImpl(
                configTypes,
                propertyRepository,
                componentAlias,
                componentType,
                environment
        );
    }
}