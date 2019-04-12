package io.microconfig.configs;

import io.microconfig.configs.sources.SpecialSource;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static io.microconfig.configs.Property.*;
import static io.microconfig.configs.sources.SpecialSource.SYSTEM_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

class PropertyTest {
    private SpecialSource source = new SpecialSource(null, SYSTEM_SOURCE);

    @Test
    void testSimpleMethods() {
        Property tempProperty = Property.parse("#var key:false", "env", source);
        assertEquals("key", tempProperty.getKey());
        assertEquals("false", tempProperty.getValue());
        assertEquals("env", tempProperty.getEnvContext());
        assertEquals(source, tempProperty.getSource());
        assertTrue(tempProperty.isTemp());

        assertEquals(tempProperty, tempProperty("key", "false", "env", source));
        assertEquals("#key=false", tempProperty.toString());

        Property notTemp = parse("key=false", "env", source);
        assertEquals(notTemp, property("key", "false", "env", source));
        assertEquals("key=false", notTemp.toString());
    }

    @Test
    void testParseException() {
        Function<String, Property> newProperty = value -> Property.parse(value, "env", source);
        assertDoesNotThrow(() -> newProperty.apply("key=value"));
        assertDoesNotThrow(() -> newProperty.apply("key:value"));
        assertThrows(IllegalArgumentException.class, () -> newProperty.apply("key"));
    }

    @Test
    void testUtilApi() {
        assertEquals(-1, separatorIndex("keyValue"));
        assertEquals(3, separatorIndex("key: Value"));
        assertEquals(3, separatorIndex("key=Value"));
        assertTrue(isTempProperty("#var"));
        assertFalse(isTempProperty("#Var"));
        assertTrue(isComment("#comment"));
        assertFalse(isComment("comment"));
    }

    @Test
    void testEscape() {
        testEscape("c:\\dir\\dir2\\\\dir3/", "c:\\\\dir\\\\dir2\\\\dir3/");
        testEscape("\\dir\\dir2\\\\dir3/", "\\\\dir\\\\dir2\\\\dir3/");
        testEscape("\\\\\\\\3\\", "\\\\\\\\3\\\\");
    }

    private void testEscape(String value, String expected) {
        assertEquals(expected, tempProperty("key", value, "", source).escapeValue());
    }

}