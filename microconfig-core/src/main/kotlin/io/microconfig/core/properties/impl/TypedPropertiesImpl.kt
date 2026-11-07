package io.microconfig.core.properties.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
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
    private val uniquePropertiesByKey = YamlScalars.indexProperties(propertyByKey.values)

    override val declaringComponent: DeclaringComponent
        get() = DeclaringComponentImpl(configType.typeName, component, environment)

    override val propertiesAsMap: Map<String, Property>
        get() = uniquePropertiesByKey

    override val propertiesAsKeyValue: Map<String, String>
        get() = uniquePropertiesByKey.mapValues { it.value.value }

    override val properties: Collection<Property>
        get() = uniquePropertiesByKey.values

    override fun resolveBy(resolver: Resolver): TypedProperties {
        val resolved = properties.map { it.resolveBy(resolver, declaringComponent) }
        return copy(propertyByKey = indexResolvedProperties(resolved))
    }

    private fun indexResolvedProperties(properties: List<Property>): Map<String, Property> {
        val indexed = YamlScalars.indexProperties(properties)
        check(indexed.size == properties.size) { "Duplicate resolved property keys" }
        return indexed
    }

    override fun withoutVars(): TypedProperties = without { it.isVar }

    override fun without(excluded: Predicate<Property>): TypedProperties =
        copy(propertyByKey = uniquePropertiesByKey.filterValues { !excluded.test(it) })

    override fun withPrefix(prefix: String): TypedProperties =
        copy(propertyByKey = uniquePropertiesByKey.filterValues { keyStartsWith(it, prefix) })

    private fun keyStartsWith(property: Property, prefix: String): Boolean =
        property.key.startsWith(prefix) ||
            property.keyFormat == PROPERTIES && PropertiesText.unescape(property.key).startsWith(prefix)

    override fun withTemplates(templates: List<Template>): TypedProperties =
        copy(templates = templates)

    override fun getPropertyWithKey(key: String): Optional<Property> =
        Optional.ofNullable(
            uniquePropertiesByKey[key] ?: findByPrefix(key) ?: findByCanonicalKey(key)
                ?: findByPropertiesKey(key) ?: findByEncodedPropertiesKey(key)
        )

    private fun findByCanonicalKey(key: String): Property? {
        yamlPropertyWithKey(YamlScalars.canonicalKeyText(key))?.let {
            return it
        }
        if ('.' !in key) return null
        yamlPropertyWithKey(YamlScalars.canonicalPathText(key))?.let {
            return it
        }
        return uniquePropertiesByKey.entries
            .firstOrNull {
                hasYamlKey(it.value) &&
                    "!!float " in it.key &&
                    YamlScalars.matchingKeyPrefix(it.key, key) == it.key
            }
            ?.value
    }

    private fun yamlPropertyWithKey(key: String): Property? =
        uniquePropertiesByKey[key]?.takeIf(::hasYamlKey)

    private fun hasYamlKey(property: Property): Boolean =
        property.keyFormat == YAML

    private fun findByPrefix(
        originalKey: String,
        candidates: Collection<Property> = properties,
    ): Property? {
        if (!originalKey.endsWith(".*")) return null
        val reference = originalKey.removeSuffix(".*")
        val prefixes =
            sequenceOf(reference, YamlScalars.canonicalPathText(reference))
                .plus(
                    candidates.asSequence()
                        .map { it.key }
                        .filter { "!!float " in it }
                        .mapNotNull { YamlScalars.matchingKeyPrefix(it, reference) }
                )
        return prefixes.firstNotNullOfOrNull { prefix ->
            candidates
                .filter { keyStartsWith(it, "$prefix.") && (prefix == reference || hasYamlKey(it)) }
                .takeIf { it.isNotEmpty() }
                ?.let { PropertyImpl.groupProperty(prefix, it, declaringComponent) }
        }
    }

    private fun findByPropertiesKey(key: String): Property? =
        properties.firstOrNull { it.keyFormat == PROPERTIES && PropertiesText.unescape(it.key) == key }

    private fun findByEncodedPropertiesKey(key: String): Property? {
        val decoded = PropertiesText.unescape(key)
        if (decoded == key) return null
        val candidates = properties.filter { it.keyFormat == PROPERTIES }
        return candidates.firstOrNull { PropertiesText.unescape(it.key) == decoded }
            ?: findByPrefix(decoded, candidates)
    }

    override fun <T> save(serializer: PropertySerializer<T>): T =
        serializer.serialize(properties, templates, configType, component, environment)

    override fun toString(): String = declaringComponent.toString()
}
