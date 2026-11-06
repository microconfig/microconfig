package io.microconfig.core.properties.serializers;

import com.google.gson.JsonParser;
import io.microconfig.core.properties.templates.BinaryTemplate;
import io.microconfig.core.properties.templates.StringTemplate;
import io.microconfig.core.properties.templates.TemplatePattern;
import io.microconfig.core.templates.Template;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigResultTest {
    @Test
    void includeComponentsContainingOnlyTextTemplates() {
        ConfigResult result = result("", List.of(textTemplate()));

        assertJsonEquals("""
                [{"service":"service","files":[
                  {"configType":"template","fileName":"template.txt","content":"hello"}
                ]}]
                """, ConfigResult.toJson(List.of(result)));
    }

    @Test
    void includeComponentsContainingOnlyBinaryTemplates() {
        Template template = new BinaryTemplate("binary", new File("source"), new File("data.bin"), new byte[]{0, 1, -1});
        ConfigResult result = result("", List.of(template));

        assertJsonEquals("""
                [{"service":"service","files":[
                  {"configType":"template","fileName":"data.bin","content":[0,1,-1]}
                ]}]
                """, ConfigResult.toJson(List.of(result)));
    }

    @Test
    void combineConfigsAndTemplatesWithoutEmptyConfigFiles() {
        ConfigResult config = result("port: 8080", List.of());
        ConfigResult templates = result("", List.of(textTemplate()));

        assertJsonEquals("""
                [{"service":"service","files":[
                  {"configType":"app","fileName":"application.yaml","content":"port: 8080"},
                  {"configType":"template","fileName":"template.txt","content":"hello"}
                ]}]
                """, ConfigResult.toJson(List.of(config, templates)));
    }

    @Test
    void omitResultsWithoutConfigsOrTemplates() {
        assertJsonEquals("[]", ConfigResult.toJson(List.of(result("", List.of()), result(null, List.of()))));
    }

    private ConfigResult result(String content, List<Template> templates) {
        return new ConfigResult("service", "app", "application.yaml", content, templates);
    }

    private Template textTemplate() {
        return new StringTemplate("text", new File("source"), new File("template.txt"),
                TemplatePattern.DEFAULT_PLACEHOLDER_PATTERN, "hello");
    }

    private void assertJsonEquals(String expected, String actual) {
        assertEquals(JsonParser.parseString(expected), JsonParser.parseString(actual));
    }
}
