//package io.microconfig.core.properties.impl;
//
//import io.microconfig.core.properties.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
//import static io.microconfig.utils.StringUtils.splitKeyValue;
//import static java.util.Arrays.asList;
//import static java.util.Collections.singletonList;
//import static java.util.Optional.empty;
//import static java.util.Optional.of;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//class TypedPropertiesImplTest {
//    Property p1 = mock(Property.class);
//    Property p2 = mock(Property.class);
//    TypedProperties subj = withProperties(asList(p1, p2));
//
//    @BeforeEach
//    void setup() {
//        when(p1.getKey()).thenReturn("key");
//        when(p1.getValue()).thenReturn("value");
//        when(p1.isTemp()).thenReturn(false);
//
//        when(p2.getKey()).thenReturn("var");
//        when(p2.getValue()).thenReturn("varlue");
//        when(p2.isTemp()).thenReturn(true);
//    }
//
//    @Test
//    void propertyWithKey() {
//        assertEquals(of(p1), subj.getPropertyWithKey("key"));
//        assertEquals(empty(), subj.getPropertyWithKey("missing"));
//    }
//
//    @Test
//    void propertiesAsKeyValue() {
//        assertEquals(
//                splitKeyValue("key=value", "var=varlue"),
//                subj.propertiesAsKeyValue()
//        );
//    }
//
//    @Test
//    void withoutTempValues() {
//        assertEquals(withProperties(singletonList(p1)), subj.withoutTempValues());
//    }
//
//    @Test
//    void resolve() {
//        Property resolved1 = mock(Property.class);
//        Property resolved2 = mock(Property.class);
//        Resolver resolver = mock(Resolver.class);
//        ComponentWithEnv root = new ComponentWithEnv(APPLICATION.getName(), "comp", "env");
//        when(p1.resolveBy(resolver, root)).thenReturn(resolved1);
//        when(p2.resolveBy(resolver, root)).thenReturn(resolved2);
//
//        assertEquals(
//                withProperties(asList(resolved1, resolved2)),
//                subj.resolveBy(resolver)
//        );
//    }
//
//    @Test
//    void configType() {
//        assertEquals(APPLICATION.getName(), subj.getConfigType());
//    }
//
//    @Test
//    void serialize() {
//        PropertySerializer<String> serializer = (p, t, c, e) -> c;
//        assertEquals("comp", subj.save(serializer));
//    }
//
//    @Test
//    void testToString() {
//        assertEquals(
//                "comp[env]",
//                subj.toString()
//        );
//    }
//
//    private TypedProperties withProperties(List<Property> properties) {
//        return new TypedPropertiesImpl(APPLICATION, "comp", "env", properties);
//    }
//}