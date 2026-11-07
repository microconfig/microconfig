package io.microconfig.core.properties.templates.impl

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplateDefinitionParser
import io.microconfig.core.properties.templates.TemplatePattern
import io.microconfig.core.properties.templates.definition.parser.impl.ArrowNotationParser
import io.microconfig.core.properties.templates.definition.parser.impl.FromToNotationParser
import io.microconfig.core.properties.templates.definition.parser.impl.SquareBracketsNotationParser
import io.microconfig.core.templates.Template
import java.util.function.UnaryOperator

class TemplatesService
@JvmOverloads
constructor(
    private val templatePattern: TemplatePattern = TemplatePattern.defaultPattern(),
    private val parsers: List<TemplateDefinitionParser> =
        listOf(
            FromToNotationParser(templatePattern),
            ArrowNotationParser(templatePattern),
            SquareBracketsNotationParser(templatePattern),
        ),
) {
    fun resolveTemplates(properties: TypedProperties, resolver: Resolver): List<Template> =
        definitions(properties.properties).map { it.resolve(resolver, properties) }

    private fun definitions(properties: Collection<Property>): List<TemplateDefinition> {
        val templateProperties =
            properties
                .filter { templatePattern.startsWithTemplatePrefix(it.key) }
                .map(::plainTextProperty)
        return parsers.flatMap { it.parse(templateProperties) }
    }

    private fun plainTextProperty(property: Property): Property =
        PropertyImpl(
            property.key,
            plainTextValue(property),
            property.isVar,
            property.configFormat,
            property.declaringComponent,
            valueFormat = ValueFormat.TEXT,
            keyFormat = property.keyFormat,
        )

    private fun plainTextValue(property: Property): String =
        when {
            property.valueFormat == ValueFormat.PROPERTIES ->
                PropertiesText.unescape(property.value)
            property.valueFormat == ValueFormat.YAML ->
                YamlScalars.unquote(property.value)
            else -> property.value
        }

    companion object {
        fun resolveTemplatesBy(resolver: Resolver): UnaryOperator<TypedProperties> {
            val service = TemplatesService()
            return UnaryOperator { properties ->
                properties.withTemplates(service.resolveTemplates(properties, resolver)).without {
                    service.templatePattern.startsWithTemplatePrefix(it.key)
                }
            }
        }
    }
}
