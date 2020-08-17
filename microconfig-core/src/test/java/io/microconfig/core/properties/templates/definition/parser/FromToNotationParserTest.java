package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.templates.TemplateDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FromToNotationParserTest {
    @TempDir
    File tempDir;

    @Test
    void singleFile() {
        ArrayList<TemplateDefinition> templateDefinitions = parseProperties(
                createProperty("mc.template.name.fromFile", "a.yaml"),
                createProperty("mc.template.name.toFile", "b.yaml"));

        assertTemplateDefinition("a.yaml", "b.yaml", templateDefinitions.get(0));
    }

    private Property createProperty(String key, String file) {
        return property(key, tempDir + "/" + file, YAML, mock(DeclaringComponent.class));
    }

    private ArrayList<TemplateDefinition> parseProperties(Property... properties) {
        return new ArrayList<>(new FromToNotationParser(defaultPattern()).parse(asList(properties)));
    }

    private void assertTemplateDefinition(String from, String to, TemplateDefinition templateDefinition) {
        assertEquals("name", templateDefinition.getTemplateName());
        assertEquals(tempDir + "/" + from, templateDefinition.getFromFile().toString());
        assertEquals(tempDir + "/" + to, templateDefinition.getToFile().toString());
    }
}