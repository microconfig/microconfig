package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySource;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.properties.impl.PropertyImpl.*;
import static org.junit.jupiter.api.Assertions.*;

class PropertyImplTest {
    PropertySource yamlSource = new FilePropertySource(new File("comp/config.yaml"), 0, true);
    Property yaml = parse("key: value", yamlSource);
    Property tempYaml = parse("#var var: value", yamlSource);

    PropertySource propSource = new FilePropertySource(new File("comp/config.properties"), 0, false);
    Property prop = parse("key=value", propSource);
    Property tempProp = parse("#var var=value", propSource);

    @Test
    void parseYaml() {
        assertEquals("key", yaml.getKey());
        assertEquals("value", yaml.getValue());
        assertEquals(yamlSource, yaml.getSource());
        assertFalse(yaml.isTemp());

        assertEquals("var", tempYaml.getKey());
        assertEquals("value", tempYaml.getValue());
        assertEquals(yamlSource, tempYaml.getSource());
        assertTrue(tempYaml.isTemp());
    }

    @Test
    void parseProp() {
        assertEquals("key", prop.getKey());
        assertEquals("value", prop.getValue());
        assertEquals(propSource, prop.getSource());
        assertFalse(prop.isTemp());

        assertEquals("var", tempProp.getKey());
        assertEquals("value", tempProp.getValue());
        assertEquals(propSource, tempProp.getSource());
        assertTrue(tempProp.isTemp());
    }

    @Test
    void failOnMissingSeparator() {
        assertThrows(IllegalArgumentException.class, () -> parse("key value", yamlSource));
    }

    @Test
    void factoryMethods() {
        assertEquals(yaml, property("key", "value", yamlSource));
    }

    @Test
    void helperMethods() {
        assertEquals(3, findSeparatorIndexIn("key: value"));
        assertEquals(3, findSeparatorIndexIn("key=value"));
        assertEquals(-1, findSeparatorIndexIn("key value"));

        assertTrue(isComment("# comment"));
        assertTrue(isTempProperty("#var bla bla"));
    }

    @Test
    void string() {
        assertEquals("key=value", yaml.toString());
        assertEquals("#var=value", tempProp.toString());
    }


    @Test
    void resolve() {
        //todo
    }

}