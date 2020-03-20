package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Components;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.properties.impl.PropertiesImpl;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentsImplTest {
    Component one = mock(Component.class);
    Properties oneProps = mock(Properties.class);
    TypedProperties oneTProps = mock(TypedProperties.class);

    Component two = mock(Component.class);
    Properties twoProps = mock(Properties.class);
    TypedProperties twoTProps = mock(TypedProperties.class);

    Components subj = new ComponentsImpl(asList(one, two));

    @Test
    void filterPropertiesForContainedComponents() {
        ConfigTypeFilter filter = mock(ConfigTypeFilter.class);
        when(one.getPropertiesFor(filter)).thenReturn(oneProps);
        when(two.getPropertiesFor(filter)).thenReturn(twoProps);
        when(oneProps.asList()).thenReturn(singletonList(oneTProps));
        when(twoProps.asList()).thenReturn(singletonList(twoTProps));

        Properties expected = new PropertiesImpl(asList(oneTProps, twoTProps));
        assertEquals(expected, subj.getPropertiesFor(filter));
    }

    @Test
    void string() {
        when(one.toString()).thenReturn("one");
        when(two.toString()).thenReturn("two");
        assertEquals("[one, two]", subj.toString());
    }
}