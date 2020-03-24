package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.impl.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.impl.PropertyImpl.*;
import static org.junit.jupiter.api.Assertions.*;

class PropertyImplTest {
    DeclaringComponent yamlSource = fileSource(new File("comp/config.yaml"), 0, true, "app", "dev");
    Property yaml = parse("key: value", PROPERTIES, yamlSource);
    Property tempYaml = parse("#var var: value", PROPERTIES, yamlSource);

    DeclaringComponent propSource = fileSource(new File("comp/config.properties"), 0, false, "app", "dev");
    Property prop = parse("key=value", PROPERTIES, propSource);
    Property tempProp = parse("#var var=value", PROPERTIES, propSource);

    @Test
    void parseYaml() {
        assertEquals("key", yaml.getKey());
        assertEquals("value", yaml.getValue());
        assertEquals(yamlSource, yaml.getDeclaringComponent());
        assertFalse(yaml.isTemp());

        assertEquals("var", tempYaml.getKey());
        assertEquals("value", tempYaml.getValue());
        assertEquals(yamlSource, tempYaml.getDeclaringComponent());
        assertTrue(tempYaml.isTemp());
    }

    @Test
    void parseProp() {
        assertEquals("key", prop.getKey());
        assertEquals("value", prop.getValue());
        assertEquals(propSource, prop.getDeclaringComponent());
        assertFalse(prop.isTemp());

        assertEquals("var", tempProp.getKey());
        assertEquals("value", tempProp.getValue());
        assertEquals(propSource, tempProp.getDeclaringComponent());
        assertTrue(tempProp.isTemp());
    }

    @Test
    void failOnMissingSeparator() {
        assertThrows(IllegalArgumentException.class, () -> parse("key value", PROPERTIES, yamlSource));
    }

    @Test
    void factoryMethods() {
        assertEquals(yaml, property("key", "value", PROPERTIES, yamlSource));
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