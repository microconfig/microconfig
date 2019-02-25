package io.microconfig.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyTest {
    @Test
    void parse() {
        Property property = Property.parse("#var key=false", "env", new Property.Source(null, null));
        assertEquals("key", property.getKey());
        assertEquals("false", property.getValue());
        assertTrue(property.isTemp());
    }
}