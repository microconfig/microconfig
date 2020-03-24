package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static io.microconfig.utils.StringUtils.splitKeyValue;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypedPropertiesImplTest {
    Property p1 = property("key");
    Property p2 = property("var");

    TypedProperties subj = withProperties(asList(p1, p2));

    @BeforeEach
    void setup() {
        when(p1.isTemp()).thenReturn(false);
        when(p2.isTemp()).thenReturn(true);
    }

    @Test
    void propertyWithKey() {
        assertEquals(of(p1), subj.getPropertyWithKey("key"));
        assertEquals(empty(), subj.getPropertyWithKey("missing"));
    }

    @Test
    void propertiesAsKeyValue() {
        assertEquals(
                splitKeyValue("key=keyValue", "var=varValue"),
                subj.propertiesAsKeyValue()
        );
    }

    @Test
    void withoutTempValues() {
        assertEquals(withProperties(singletonList(p1)), subj.withoutTempValues());
    }

    @Test
    void resolve() {
        Property resolved1 = property("resolved1");
        Property resolved2 = property("resolved2");

        Resolver resolver = mock(Resolver.class);
        DeclaringComponent root = new DeclaringComponentImpl(APPLICATION.getName(), "comp", "env");
        when(p1.resolveBy(resolver, root)).thenReturn(resolved1);
        when(p2.resolveBy(resolver, root)).thenReturn(resolved2);

        assertEquals(
                withProperties(asList(resolved1, resolved2)),
                subj.resolveBy(resolver)
        );
    }

    @Test
    void configType() {
        assertEquals(APPLICATION.getName(), subj.getConfigType());
    }

    @Test
    void serialize() {
        PropertySerializer<String> serializer = (p, t, c, e) -> c;
        assertEquals("comp", subj.save(serializer));
    }

    @Test
    void testToString() {
        assertEquals(
                "comp[env]",
                subj.toString()
        );
    }

    private TypedProperties withProperties(Collection<Property> properties) {
        return new TypedPropertiesImpl(APPLICATION, "comp", "env",
                properties.stream().collect(toLinkedMap(Property::getKey, identity()))
        );
    }

    private Property property(String key) {
        Property property = mock(Property.class);
        when(property.getKey()).thenReturn(key);
        when(property.getValue()).thenReturn(key + "Value");
        return property;
    }
}