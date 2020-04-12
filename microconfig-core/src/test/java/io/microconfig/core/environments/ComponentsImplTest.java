package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.core.properties.PropertiesImpl;
import io.microconfig.core.properties.TypedProperties;
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
    PropertiesFactory factory = mock(PropertiesFactory.class);

    Components subj = new ComponentsImpl(asList(one, two), factory);

    @Test
    void filterPropertiesForContainedComponents() {
        ConfigTypeFilter filter = mock(ConfigTypeFilter.class);
        when(one.getPropertiesFor(filter)).thenReturn(oneProps);
        when(two.getPropertiesFor(filter)).thenReturn(twoProps);
        when(oneProps.asTypedProperties()).thenReturn(singletonList(oneTProps));
        when(twoProps.asTypedProperties()).thenReturn(singletonList(twoTProps));

        Properties expected = new PropertiesImpl(asList(oneTProps, twoTProps));
        when(factory.flat(asList(oneProps, twoProps))).thenReturn(expected);
        assertEquals(expected, subj.getPropertiesFor(filter));
    }

    @Test
    void string() {
        when(one.toString()).thenReturn("one");
        when(two.toString()).thenReturn("two");
        assertEquals("[one, two]", subj.toString());
    }
}