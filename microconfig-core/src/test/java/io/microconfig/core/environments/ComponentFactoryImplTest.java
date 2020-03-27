package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.properties.PropertiesFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ComponentFactoryImplTest {
    ConfigTypeRepository configTypeRepository = mock(ConfigTypeRepository.class);
    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);

    ComponentFactory subj = new ComponentFactoryImpl(configTypeRepository, propertiesFactory);

    @Test
    void createComponent() {
        String name = "name";
        String originalName = "originalName";
        String env = "env";
        assertEquals(
                new ComponentImpl(configTypeRepository, propertiesFactory, name, originalName, env),
                subj.createComponent(name, originalName, env)
        );
    }

}