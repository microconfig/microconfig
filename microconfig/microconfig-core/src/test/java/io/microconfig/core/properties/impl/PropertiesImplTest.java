package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static io.microconfig.core.properties.impl.PropertiesImpl.composite;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;
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
        TypedProperties r1 = mock(TypedProperties.class);
        TypedProperties r2 = mock(TypedProperties.class);
        Resolver resolver = mock(Resolver.class);
        when(tp1.resolveBy(resolver)).thenReturn(r1);
        when(tp2.resolveBy(resolver)).thenReturn(r2);

        assertEquals(
                new PropertiesImpl(asList(r1, r2)),
                subj.resolveBy(resolver)
        );
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
    void getProperties() {
        Property p3 = getProperty(3);
        Property p1 = getProperty(1);
        Property p2 = getProperty(2);
        when(tp1.getProperties()).thenReturn(asList(p3, p1));
        when(tp2.getProperties()).thenReturn(singletonList(p2));

        assertEquals(
                of(p3, p1, p2).collect(toLinkedMap(Property::getKey, identity())),
                subj.getPropertiesAsMap()
        );
        assertEquals(
                of(p3, p1, p2).collect(toLinkedMap(Property::getKey, Property::getValue)),
                subj.getPropertiesAsKeyValue()
        );
        assertEquals(
                asList(p3, p1, p2),
                subj.getProperties()
        );
    }
    
    @Test
    void getPropertyWithKey() {
        Property p1 = getProperty(1);
        when(tp2.getPropertyWithKey("key1")).thenReturn(Optional.of(p1));
        assertEquals(Optional.of(p1), subj.getPropertyWithKey("key1"));
        assertEquals(Optional.empty(), subj.getPropertyWithKey("key4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void save() {
        PropertySerializer<Integer> serializer = mock(PropertySerializer.class);
        when(tp1.save(serializer)).thenReturn(1);
        when(tp2.save(serializer)).thenReturn(2);
        assertEquals(
                asList(1, 2),
                subj.save(serializer)
        );
    }

    @Test
    void asTypedProperties() {
        assertEquals(asList(tp1, tp2), subj.asTypedProperties());
    }


    private Property getProperty(int order) {
        Property p = mock(Property.class);
        when(p.getKey()).thenReturn("key" + order);
        when(p.getValue()).thenReturn("value" + order);
        return p;
    }
}