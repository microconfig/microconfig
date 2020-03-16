package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.microconfig.domain.impl.configtypes.CompositeConfigTypeRepository.composite;
import static io.microconfig.domain.impl.configtypes.StandardConfigType.*;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeConfigTypeRepositoryTest {
    @Mock
    private ConfigTypeRepository r1;
    @Mock
    private ConfigTypeRepository r2;
    @Mock
    private ConfigTypeRepository r3;

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