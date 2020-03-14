package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.el.ExpressionResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolver;
import org.junit.jupiter.api.Test;

import static io.microconfig.domain.impl.properties.PropertyImpl.property;
import static io.microconfig.domain.impl.properties.resolvers.composite.CompositeResolver.chainOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyImplTest {
    @Test
    void testResolve() {
        Property original = property("key", "I'm #{#{1 + 2} * #{10 - 4}}!");
        Property resolved = original.resolveBy(new ExpressionResolver());
        assertEquals("I'm 18!", resolved.getValue());
    }

    @Test
    void testCompositeResolve() {
//        Property original = property("key", "I'm #{#{c1@k1} * #{c2@k1}}!");
        Property original = property("key", "I'm #{#{1 + 2} * #{10 - 4}}!");
        Property resolved = original.resolveBy(chainOf(new PlaceholderResolver(), new ExpressionResolver()));
        assertEquals("I'm 18!", resolved.getValue());
    }
}