package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.Property;

import java.util.Collection;

public interface TemplateDefinitionParser {
    Collection<TemplateDefinition> parse(Collection<Property> componentProperties);
}
