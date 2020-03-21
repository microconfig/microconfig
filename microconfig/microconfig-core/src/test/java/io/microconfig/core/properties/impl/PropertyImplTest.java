package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.core.properties.impl.PropertyImpl.*;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class PropertyImplTest {
    PropertySource yamlSource = new FilePropertySource(new File("comp/config.yaml"), 0, true);
    Property yaml = parse("key: value", "env", yamlSource);
    Property tempYaml = parse("#var var: value", "env", yamlSource);

    PropertySource propSource = new FilePropertySource(new File("comp/config.properties"), 0, false);
    Property prop = parse("key=value", "env", propSource);
    Property tempProp = parse("#var var=value", "env", propSource);

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
        assertThrows(IllegalArgumentException.class, () -> parse("key value", "env", yamlSource));
    }

    @Test
    void factoryMethods() {
        assertEquals(yaml, property("key", "value", "env", yamlSource));
        assertEquals(tempYaml, tempProperty("var", "value", "env", yamlSource));
    }

    @Test
    void helperMethods() {
        assertEquals(3, findKeyValueSeparatorIndexIn("key: value"));
        assertEquals(3, findKeyValueSeparatorIndexIn("key=value"));
        assertEquals(-1, findKeyValueSeparatorIndexIn("key value"));

        assertTrue(isComment("# comment"));
        assertTrue(isTempProperty("#var bla bla"));
    }

    @Test
    void asMap() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("key", "value");
        expected.put("var", "value");

        assertEquals(expected, asKeyValue(asList(yaml, tempYaml)));
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