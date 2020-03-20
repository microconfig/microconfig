package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertiesFactory;
import org.junit.jupiter.api.Test;

import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.core.configtypes.impl.StandardConfigType.DEPLOY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentImplTest {
    ConfigTypeRepository configTypeRepository = mock(ConfigTypeRepository.class);
    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);
    ConfigTypeFilter filter = mock(ConfigTypeFilter.class);
    ComponentImpl subj = new ComponentImpl(configTypeRepository, propertiesFactory, "name", "original", "env");

    @Test
    void filterProperties() {
        Properties properties = mock(Properties.class);
        when(configTypeRepository.getConfigTypes()).thenReturn(asList(APPLICATION, DEPLOY));
        when(filter.selectTypes(asList(APPLICATION, DEPLOY))).thenReturn(singletonList(DEPLOY));
        when(propertiesFactory.getPropertiesOf("name", "original", "env", singletonList(DEPLOY))).thenReturn(properties);

        assertEquals(properties, subj.getPropertiesFor(filter));
    }

    @Test
    void string() {
        assertEquals("name", subj.toString());
    }
}