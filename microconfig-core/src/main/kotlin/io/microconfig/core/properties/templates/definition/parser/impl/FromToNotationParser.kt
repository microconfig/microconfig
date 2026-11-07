package io.microconfig.core.properties.templates.definition.parser.impl

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplateDefinitionParser
import io.microconfig.core.properties.templates.TemplatePattern

class FromToNotationParser(private val pattern: TemplatePattern) : TemplateDefinitionParser {
    override fun parse(componentProperties: Collection<Property>): Collection<TemplateDefinition> =
        definitions(componentProperties).values

    private fun definitions(
        properties: Collection<Property>
    ): Map<Pair<String, String>, TemplateDefinition> {
        val definitions = linkedMapOf<Pair<String, String>, TemplateDefinition>()
        properties.forEach { property ->
            val key = property.key
            when (fieldOf(key)) {
                "fromFile" ->
                    getOrCreate(key, definitions).setFromFile(property.value.trim { it <= ' ' })
                "toFile" ->
                    getOrCreate(key, definitions).setToFile(property.value.trim { it <= ' ' })
            }
        }
        return definitions
    }

    private fun fieldOf(key: String): String? {
        val prefix = pattern.templatePrefixes.firstOrNull(key::startsWith) ?: return null
        return key.removePrefix(prefix).substringAfterLast('.', "")
    }

    private fun getOrCreate(
        key: String,
        definitions: MutableMap<Pair<String, String>, TemplateDefinition>,
    ): TemplateDefinition {
        val type = pattern.extractTemplateType(key)
        val name = pattern.extractTemplateName(key)
        return definitions.getOrPut(type to name) { TemplateDefinition(type, name, pattern) }
    }
}
