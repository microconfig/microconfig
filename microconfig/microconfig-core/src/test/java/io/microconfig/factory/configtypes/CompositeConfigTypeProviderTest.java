package io.microconfig.factory.configtypes;

import io.microconfig.factory.ConfigType;
import io.microconfig.factory.ConfigsTypeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static io.microconfig.factory.configtypes.CompositeConfigTypeProvider.composite;
import static io.microconfig.factory.configtypes.StandardConfigTypes.APPLICATION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeConfigTypeProviderTest {
    @Mock
    private ConfigsTypeProvider provider1;
    @Mock
    private ConfigsTypeProvider provider2;
    @Mock
    private ConfigsTypeProvider provider3;

    private ConfigsTypeProvider configTypeProvider;

    @BeforeEach
    void setUp() {
        configTypeProvider = composite(provider1, provider2, provider3);
    }

    @Test
    void test() {
        File dir = new File("someDir");
        when(provider1.getConfigTypes(dir)).thenReturn(emptyList());
        List<ConfigType> type2 = singletonList(APPLICATION.getType());
        when(provider2.getConfigTypes(dir)).thenReturn(type2);

        assertSame(type2, configTypeProvider.getConfigTypes(dir));
    }
}