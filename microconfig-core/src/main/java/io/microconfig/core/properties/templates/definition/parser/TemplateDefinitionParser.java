package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.templates.TemplateDefinition;

import java.util.Collection;

public interface TemplateDefinitionParser {
    void add(String key, String value);

    Collection<TemplateDefinition> getDefinitions();
}
