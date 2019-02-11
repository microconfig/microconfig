package io.microconfig.properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PropertyTest {
    @Test
    public void parse() {
        Property property = Property.parse("#var key=false", "env", new Property.Source(null, null));
        assertEquals("key", property.getKey());
        assertEquals("false", property.getValue());
        assertTrue(property.isTemp());
    }
}