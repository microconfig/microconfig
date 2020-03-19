package io.microconfig.core.properties;

import io.microconfig.core.resolvers.expression.ExpressionResolver;
import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.impl.PropertyImpl.property;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyImplTest {
    @Test
    void testResolve() {
        Property original = property("key", "I'm #{#{1 + 2} * #{10 - 4}}!", "dev", null);
        Property resolved = original.resolveBy(new ExpressionResolver(), "app");
        assertEquals("I'm 18!", resolved.getValue());
    }
}