package io.microconfig.core.properties;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.PropertyImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertEquals(8, findSeparatorIndexIn("keyValue=value"));
        assertEquals(-1, findSeparatorIndexIn("key value"));

        assertTrue(isComment("# comment"));
        assertFalse(isComment("comment"));
        assertTrue(isTempProperty("#var bla bla"));
        assertFalse(isTempProperty("var bla"));
    }

    @Test
    void string() {
        assertEquals("key=value", yaml.toString());
        assertEquals("#var=value", tempProp.toString());
    }

    @Test
    void resolve() {
        String resolved = "resolved";
        Resolver resolver = mock(Resolver.class);
        when(resolver.resolve("value", yamlSource, propSource)).thenReturn(resolved);
        assertEquals(
                property("key", resolved, PROPERTIES, yamlSource),
                yaml.resolveBy(resolver, propSource)
        );
    }
}