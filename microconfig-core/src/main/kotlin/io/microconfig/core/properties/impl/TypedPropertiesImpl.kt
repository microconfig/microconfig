package io.microconfig.core.properties.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.name
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.templates.Template
import java.util.Optional
import java.util.function.Predicate

data class TypedPropertiesImpl(
    override val configType: ConfigType,
    private val component: String,
    private val environment: String,
    private val propertyByKey: Map<String, Property>,
    override val templates: List<Template>,
) : TypedProperties {
    override val declaringComponent: DeclaringComponent
        get() = DeclaringComponentImpl(configType.name, component, environment)

    override val propertiesAsMap: Map<String, Property>
        get() = propertyByKey

    override val propertiesAsKeyValue: Map<String, String>
        get() = propertyByKey.mapValues { it.value.value }

    override val properties: Collection<Property>
        get() = propertyByKey.values

    override fun resolveBy(resolver: Resolver): TypedProperties {
        val resolved = properties.map { it.resolveBy(resolver, declaringComponent) }
        return copy(propertyByKey = indexResolvedProperties(resolved))
    }

    private fun indexResolvedProperties(properties: List<Property>): Map<String, Property> {
        val indexed = properties.associateBy { it.key }
        check(indexed.size == properties.size) { "Duplicate resolved property keys" }
        return indexed
    }

    override fun withoutVars(): TypedProperties = without { it.isVar }

    override fun without(excluded: Predicate<Property>): TypedProperties =
        copy(propertyByKey = propertyByKey.filterValues { !excluded.test(it) })

    override fun withPrefix(prefix: String): TypedProperties =
        copy(propertyByKey = propertyByKey.filterKeys { it.startsWith(prefix) })

    override fun withTemplates(templates: List<Template>): TypedProperties =
        copy(templates = templates)

    override fun getPropertyWithKey(key: String): Optional<Property> =
        Optional.ofNullable(propertyByKey[key] ?: findByPrefix(key) ?: findByCanonicalKey(key))

    private fun findByCanonicalKey(key: String): Property? {
        yamlPropertyWithKey(YamlScalars.canonicalKeyText(key))?.let {
            return it
        }
        if ('.' !in key) return null
        yamlPropertyWithKey(YamlScalars.canonicalPathText(key))?.let {
            return it
        }
        return propertyByKey.entries
            .firstOrNull {
                hasYamlKey(it.value) &&
                    "!!float " in it.key &&
                    YamlScalars.matchingKeyPrefix(it.key, key) == it.key
            }
            ?.value
    }

    private fun yamlPropertyWithKey(key: String): Property? =
        propertyByKey[key]?.takeIf(::hasYamlKey)

    private fun hasYamlKey(property: Property): Boolean =
        property.configFormat == YAML &&
            !PropertiesText.isPropertiesText(property.declaringComponent)

    private fun findByPrefix(originalKey: String): Property? {
        if (!originalKey.endsWith(".*")) return null
        val reference = originalKey.removeSuffix(".*")
        val prefixes =
            sequenceOf(reference, YamlScalars.canonicalPathText(reference))
                .plus(
                    propertyByKey.keys
                        .asSequence()
                        .filter { "!!float " in it }
                        .mapNotNull { YamlScalars.matchingKeyPrefix(it, reference) }
                )
        return prefixes.firstNotNullOfOrNull { prefix ->
            properties
                .filter { it.key.startsWith("$prefix.") && (prefix == reference || hasYamlKey(it)) }
                .takeIf { it.isNotEmpty() }
                ?.let { PropertyImpl.groupProperty(prefix, it, declaringComponent) }
        }
    }

    override fun <T> save(serializer: PropertySerializer<T>): T =
        serializer.serialize(properties, templates, configType, component, environment)

    override fun toString(): String = declaringComponent.toString()
}
