package io.microconfig.core.properties.templates

import io.microconfig.core.properties.Property

fun interface TemplateDefinitionParser {
    fun parse(componentProperties: Collection<Property>): Collection<TemplateDefinition>
}
