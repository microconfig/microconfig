package io.microconfig.core.properties.templates;

import java.util.Collection;

public interface TemplateDefinitionParser {
    void add(String key, String value);

    Collection<TemplateDefinition> getDefinitions();
}
