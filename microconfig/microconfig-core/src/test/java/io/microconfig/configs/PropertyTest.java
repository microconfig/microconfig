package io.microconfig.configs;

import io.microconfig.configs.sources.SpecialSource;
import org.junit.jupiter.api.Test;

import static io.microconfig.configs.sources.SpecialSource.SYSTEM_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyTest {
    @Test
    void parse() {
        Property property = Property.parse("#var key:false", "env", new SpecialSource(null, null));
        assertEquals("key", property.getKey());
        assertEquals("false", property.getValue());
        assertTrue(property.isTemp());
    }

    @Test
    void testEscape() {
        testEscape("c:\\dir\\dir2\\\\dir3/", "c:\\\\dir\\\\dir2\\\\dir3/");
        testEscape("\\dir\\dir2\\\\dir3/", "\\\\dir\\\\dir2\\\\dir3/");
        testEscape("\\\\\\\\3\\", "\\\\\\\\3\\\\");
    }

    private void testEscape(String value, String expected) {
        assertEquals(expected, Property.tempProperty("key", value, "", new SpecialSource(null, SYSTEM_SOURCE)).escapeValue());
    }
}