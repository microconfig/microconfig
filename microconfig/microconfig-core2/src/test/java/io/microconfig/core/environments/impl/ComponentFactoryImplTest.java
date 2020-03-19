package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.PropertiesFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ComponentFactoryImplTest {
    ConfigTypeRepository configTypeRepository = mock(ConfigTypeRepository.class);
    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);

    ComponentFactoryImpl subj = new ComponentFactoryImpl(configTypeRepository, propertiesFactory);

    @Test
    void createComponent() {
        Component expected = new ComponentImpl(configTypeRepository, propertiesFactory, "name", "type", "env");
        assertEquals(expected, subj.createComponent("name", "type", "env"));
    }

}