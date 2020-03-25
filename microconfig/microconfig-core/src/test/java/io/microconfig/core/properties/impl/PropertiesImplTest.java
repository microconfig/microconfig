package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.TypedProperties;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static io.microconfig.core.properties.impl.PropertiesImpl.composite;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertiesImplTest {
    TypedProperties tp1 = mock(TypedProperties.class);
    TypedProperties tp2 = mock(TypedProperties.class);
    Properties subj = new PropertiesImpl(asList(tp1, tp2));

    @Test
    void compositeFactoryMethod() {
        Function<TypedProperties, Properties> prop = tp -> {
            Properties p = mock(Properties.class);
            when(p.asTypedProperties()).thenReturn(singletonList(tp1));
            return p;
        };

        assertEquals(
                subj,
                composite(asList(prop.apply(tp1), prop.apply(tp2)))
        );
    }

    @Test
    void resolveBy() {
    }

    @Test
    void withoutTempValues() {
        TypedProperties r1 = mock(TypedProperties.class);
        TypedProperties r2 = mock(TypedProperties.class);
        when(tp1.withoutTempValues()).thenReturn(r1);
        when(tp2.withoutTempValues()).thenReturn(r2);

        assertEquals(
                new PropertiesImpl(asList(r1, r2)),
                subj.withoutTempValues()
        );
    }

    @Test
    void getPropertiesAsMap() {
    }

    @Test
    void getPropertiesAsKeyValue() {
    }

    @Test
    void getProperties() {
    }

    @Test
    void getPropertyWithKey() {
    }

    @Test
    void save() {
    }

    @Test
    void asTypedProperties() {
        assertEquals(asList(tp1, tp2), subj.asTypedProperties());
    }
}