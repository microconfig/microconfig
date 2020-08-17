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
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ArrowNotationParserTest {
    @TempDir
    File tempDir;

    @Test
    void singleFile() {
        List<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.name", "a.yaml", "b.yaml");
        assertTemplateDefinition("a.yaml", "b.yaml", templateDefinitions);
    }

    @Test
    void multipleFiles() throws IOException {
        createFile("first.yaml");
        createFile("second.yaml");

        List<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.name", "*", "dir");
        assertEquals(2, templateDefinitions.size());
        assertTemplateDefinition("first.yaml", "dir/first.yaml", templateDefinitions);
        assertTemplateDefinition("second.yaml", "dir/second.yaml", templateDefinitions);
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

    private void assertTemplateDefinition(String from, String to, List<TemplateDefinition> templateDefinitions) {
        templateDefinitions.stream()
                .filter(td -> td.getTemplateName().equals("name"))
                .filter(td -> unixLikePath(td.getFromFile().toString()).equals(unixLikePath(new File(tempDir, from).toString())))
                .filter(td -> unixLikePath(td.getToFile().toString()).equals(unixLikePath(new File(tempDir, to).toString())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Template " + from + "->" + to + " not found"));
    }

    private void createFile(String s) throws IOException {
        new File(tempDir + "/" + s).createNewFile();
    }
}