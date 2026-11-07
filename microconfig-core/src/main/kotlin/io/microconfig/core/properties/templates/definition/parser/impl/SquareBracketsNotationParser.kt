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
        val (from, to) = ArrowNotation.pathsIn(property) ?: return emptyList()
        val names =
            Regex(".*\\.\\[(.*)]").matchEntire(property.key)?.groupValues?.get(1)
                ?: return emptyList()
        val type = pattern.extractTemplateType(property.key)
        return names
            .split(',')
            .dropLastWhile { it.isEmpty() }
            .map { name -> createDefinition(type, name, from, to) }
    }

    private fun correctNotation(property: Property): Boolean {
        val prefix = pattern.templatePrefixes.firstOrNull(property.key::startsWith) ?: return false
        val name = property.key.removePrefix(prefix)
        return name.startsWith('[') && name.endsWith(']') && '.' !in name && '*' !in property.value
    }

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
