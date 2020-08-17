package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.templates.TemplateDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SquareBracketsNotationParserTest {
    @TempDir
    File tempDir;

    @Test
    void multipleTemplateNames() {
        List<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.[name1,name2]", "a.yaml", "b.yaml");
        assertTemplateDefinition("name1", "a.yaml", "b.yaml", templateDefinitions.get(0));
        assertTemplateDefinition("name2", "a.yaml", "b.yaml", templateDefinitions.get(1));
    }

    @Test
    void ignoreIfAsteriskInFromFile() {
        List<TemplateDefinition> templateDefinitions = parseArrowProperty("mc.template.[name1,name2]", "*", "dir");
        assertEquals(0, templateDefinitions.size());
    }

    private List<TemplateDefinition> parseArrowProperty(String key, String from, String to) {
        Property property = property(key, tempDir + "/" + from + " -> " + tempDir + "/" + to, YAML, mock(DeclaringComponent.class));
        return new ArrayList<>(new SquareBracketsNotationParser(defaultPattern()).parse(singletonList(property)));
    }

    private void assertTemplateDefinition(String name, String from, String to, TemplateDefinition templateDefinition) {
        assertEquals(name, templateDefinition.getTemplateName());
        assertEquals(unixLikePath(tempDir + "/" + from), unixLikePath(templateDefinition.getFromFile().toString()));
        assertEquals(unixLikePath(tempDir + "/" + to), unixLikePath(templateDefinition.getToFile().toString()));
    }
}