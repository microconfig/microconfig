package io.microconfig.core.configtypes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.core.configtypes.CompositeConfigTypeRepository.composite;
import static io.microconfig.core.configtypes.StandardConfigType.*;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeConfigTypeRepositoryTest {
    ConfigTypeRepository r1 = mock(ConfigTypeRepository.class);
    ConfigTypeRepository r2 = mock(ConfigTypeRepository.class);
    ConfigTypeRepository r3 = mock(ConfigTypeRepository.class);

    @Test
    void returnsFirstNotEmptyTypes() {
        List<ConfigType> r2Types = asList(APPLICATION, DEPLOY);
        List<ConfigType> r3Types = asList(SECRET, PROCESS);
        when(r2.getConfigTypes()).thenReturn(r2Types);
        when(r3.getConfigTypes()).thenReturn(r3Types);

        assertSame(r2Types, composite(r1, r2, r3).getConfigTypes());
        assertSame(r3Types, composite(r1, r3, r2).getConfigTypes());
        assertSame(r2Types, composite(r2, r1).getConfigTypes());
    }

    @Test
    void testExceptionIfNoTypesConfigured() {
        assertThrows(IllegalStateException.class, () -> composite(r1).getConfigTypes());
    }
}