package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.Property;
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

    @Override
    public Collection<TemplateDefinition> parse(Collection<Property> componentProperties) {
        Map<String, TemplateDefinition> templates = new LinkedHashMap<>();
        componentProperties.forEach(p -> {
            String key = p.getKey();
            String value = p.getValue();
            if (key.endsWith(".fromFile")) {
                getOrCreate(key, templates).setFromFile(value.trim());
            } else if (key.endsWith(".toFile")) {
                getOrCreate(key, templates).setToFile(value.trim());
            }
        });
        return templates.values();
    }

    private TemplateDefinition getOrCreate(String key, Map<String, TemplateDefinition> templates) {
        return templates.computeIfAbsent(
                templatePattern.extractTemplateName(key),
                templateName -> new TemplateDefinition(templatePattern.extractTemplateType(key), templateName, templatePattern)
        );
    }
}
