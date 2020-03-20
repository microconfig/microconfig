package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySerializer;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.utils.StringUtils.splitKeyValue;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypedPropertiesImplTest {
    Property key = mock(Property.class);
    Property var = mock(Property.class);
    Resolver resolver = mock(Resolver.class);
    PropertySerializer<String> serializer = (p, t, c, e) -> c;

    TypedProperties subj = withProperties(asList(key, var));

    @BeforeEach
    void setup() {
        when(key.getKey()).thenReturn("key");
        when(key.getValue()).thenReturn("value");
        when(key.isTemp()).thenReturn(false);

        when(var.getKey()).thenReturn("var");
        when(var.getValue()).thenReturn("varlue");
        when(var.isTemp()).thenReturn(true);
    }

    @Test
    void propertyWithKey() {
        assertEquals(of(key), subj.getPropertyWithKey("key"));
        assertEquals(empty(), subj.getPropertyWithKey("missing"));
    }

    @Test
    void propertiesMap() {
        assertEquals(
                splitKeyValue("key=value", "var=varlue"),
                subj.propertiesAsKeyValue()
        );
    }

    @Test
    void withoutTemp() {
        assertEquals(withProperties(singletonList(key)), subj.withoutTempValues());
    }

    @Test
    void resolve() {
        Property keyR = mock(Property.class);
        Property varR = mock(Property.class);
        when(key.resolveBy(resolver, APPLICATION.getName())).thenReturn(keyR);
        when(var.resolveBy(resolver, APPLICATION.getName())).thenReturn(varR);

        TypedProperties expected = withProperties(asList(keyR, varR));
        assertEquals(expected, subj.resolveBy(resolver));
    }

    @Test
    void configType() {
        assertEquals(APPLICATION.getName(), subj.getConfigType());
    }

    @Test
    void serialize() {
        assertEquals("comp", subj.save(serializer));
    }

    private TypedProperties withProperties(List<Property> properties) {
        return new TypedPropertiesImpl("comp", "env", APPLICATION, properties);
    }
}