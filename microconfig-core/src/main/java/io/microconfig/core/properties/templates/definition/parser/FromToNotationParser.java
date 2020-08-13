package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.templates.TemplateDefinition;
import io.microconfig.core.properties.templates.TemplateDefinitionParser;
import io.microconfig.core.properties.templates.TemplatePattern;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class FromToNotationParser implements TemplateDefinitionParser {
    private final TemplatePattern templatePattern;
    private final Map<String, TemplateDefinition> templates = new LinkedHashMap<>();

    @Override
    public void add(String key, String value) {
        if (key.endsWith(".fromFile")) {
            getOrCreate(key).setFromFile(value.trim());
        } else if (key.endsWith(".toFile")) {
            getOrCreate(key).setToFile(value.trim());
        }
    }

    @Override
    public Collection<TemplateDefinition> getDefinitions() {
        return templates.values();
    }

    private TemplateDefinition getOrCreate(String key) {
        return templates.computeIfAbsent(
                templatePattern.extractTemplateName(key),
                templateName -> new TemplateDefinition(templatePattern.extractTemplateType(key), templateName, templatePattern)
        );
    }
}
