package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.templates.TemplateDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ArrowNotationParserTest {
    @TempDir
    File tempDir;

    @Test
    void singleFile() {
        List<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.name", "a.yaml", "b.yaml");
        assertTemplateDefinition("a.yaml", "b.yaml", templateDefinitions.get(0));
    }

    @Test
    void multipleFiles() throws IOException {
        createFile("first.yaml");
        createFile("second.yaml");

        List<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.name", "*", "dir");
        assertTemplateDefinition("first.yaml", "dir/first.yaml", templateDefinitions.get(0));
        assertTemplateDefinition("second.yaml", "dir/second.yaml", templateDefinitions.get(1));
    }

    @Test
    void ifKeyContainsSquareBrackets_Ignore() {
        Collection<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.[name]", "a.yaml", "b.yaml");
        assertEquals(0, templateDefinitions.size());
    }

    private List<TemplateDefinition> parseArrowProperty(String key, String from, String to) {
        Property property = property(key, tempDir + "/" + from + " -> " + tempDir + "/" + to, YAML, mock(DeclaringComponent.class));
        return new ArrayList<>(new ArrowNotationParser(defaultPattern()).parse(singletonList(property)));
    }

    private void assertTemplateDefinition(String from, String to, TemplateDefinition templateDefinition) {
        assertEquals("name", templateDefinition.getTemplateName());
        assertEquals(tempDir + "/" + from, templateDefinition.getFromFile().toString());
        assertEquals(tempDir + "/" + to, templateDefinition.getToFile().toString());
    }

    private void createFile(String s) throws IOException {
        new File(tempDir + "/" + s).createNewFile();
    }
}