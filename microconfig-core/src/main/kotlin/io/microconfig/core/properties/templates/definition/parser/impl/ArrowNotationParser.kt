package io.microconfig.core.properties.templates.definition.parser.impl

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplateDefinitionParser
import io.microconfig.core.properties.templates.TemplatePattern
import java.io.File
import java.io.IOException
import java.nio.file.Files

class ArrowNotationParser(private val pattern: TemplatePattern) : TemplateDefinitionParser {
    override fun parse(componentProperties: Collection<Property>): Collection<TemplateDefinition> =
        componentProperties.flatMap(::processProperty)

    private fun processProperty(property: Property): List<TemplateDefinition> {
        if (!correctNotation(property.key)) return emptyList()
        val (from, to) = ArrowNotation.pathsIn(property) ?: return emptyList()
        return if (from.endsWith("/*")) processDirectory(property.key, from, to)
        else listOf(createTemplate(property.key, from, to))
    }

    private fun correctNotation(key: String): Boolean {
        val prefix = pattern.templatePrefixes.firstOrNull(key::startsWith) ?: return false
        val name = key.removePrefix(prefix)
        return '.' !in name && '[' !in name
    }

    private fun processDirectory(key: String, from: String, to: String): List<TemplateDefinition> {
        val root = File(from.removeSuffix("/*")).toPath()
        return try {
            Files.walk(root).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .map { createTemplate(key, it.toString(), "$to/${root.relativize(it)}") }
                    .toList()
            }
        } catch (error: IOException) {
            throw IllegalStateException("Can't get templates from dir $from", error)
        }
    }

    private fun createTemplate(key: String, from: String, to: String): TemplateDefinition =
        TemplateDefinition(
            pattern.extractTemplateType(key),
            pattern.extractTemplateName(key),
            pattern,
        )
            .apply {
                setFromFile(from)
                setToFile(to)
            }
}
