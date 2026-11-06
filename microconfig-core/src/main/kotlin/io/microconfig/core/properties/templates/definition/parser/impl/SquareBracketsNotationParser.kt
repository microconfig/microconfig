package io.microconfig.core.properties.templates.definition.parser.impl

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplateDefinitionParser
import io.microconfig.core.properties.templates.TemplatePattern

class SquareBracketsNotationParser(private val pattern: TemplatePattern) :
    TemplateDefinitionParser {
    override fun parse(componentProperties: Collection<Property>): Collection<TemplateDefinition> =
        componentProperties.flatMap(::processProperty)

    private fun processProperty(property: Property): List<TemplateDefinition> {
        if (!correctNotation(property)) return emptyList()
        val parts = property.value.trim { it <= ' ' }.split(" -> ").dropLastWhile { it.isEmpty() }
        if (parts.size != 2) return emptyList()
        val names =
            Regex(".*\\.\\[(.*)]").matchEntire(property.key)?.groupValues?.get(1)
                ?: return emptyList()
        val type = pattern.extractTemplateType(property.key)
        return names
            .split(',')
            .dropLastWhile { it.isEmpty() }
            .map { name -> createDefinition(type, name, parts[0], parts[1]) }
    }

    private fun correctNotation(property: Property): Boolean =
        '[' in property.key &&
            property.key.endsWith(pattern.extractTemplateName(property.key)) &&
            '*' !in property.value

    private fun createDefinition(
        type: String,
        name: String,
        from: String,
        to: String,
    ): TemplateDefinition =
        TemplateDefinition(type, name, pattern).apply {
            setFromFile(from)
            setToFile(to)
        }
}
