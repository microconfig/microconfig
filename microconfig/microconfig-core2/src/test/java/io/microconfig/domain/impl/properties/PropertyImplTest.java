package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.expression.ExpressionResolver;
import org.junit.jupiter.api.Test;

import static io.microconfig.domain.impl.properties.PropertyImpl.property;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyImplTest {
    @Test
    void testResolve() {
        Property original = property("key", "I'm #{#{1 + 2} * #{10 - 4}}!");
        Property resolved = original.resolveBy(new ExpressionResolver());
        assertEquals("I'm 18!", resolved.getValue());
    }
}