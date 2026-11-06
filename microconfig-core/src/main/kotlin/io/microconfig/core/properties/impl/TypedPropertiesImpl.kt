package io.microconfig.core.properties.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.name
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.templates.Template
import java.util.Optional
import java.util.function.Predicate

data class TypedPropertiesImpl(
    override val configType: ConfigType,
    private val component: String,
    private val environment: String,
    private val propertyByKey: Map<String, Property>,
    private val templates: List<Template>,
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
        Optional.ofNullable(propertyByKey[key] ?: findByPrefix(key))

    private fun findByPrefix(originalKey: String): Property? {
        if (!originalKey.endsWith(".*")) return null
        val key = originalKey.removeSuffix(".*")
        val children = withPrefix("$key.").properties
        return children
            .takeIf { it.isNotEmpty() }
            ?.let { PropertyImpl.groupProperty(key, it, declaringComponent) }
    }

    override fun <T> save(serializer: PropertySerializer<T>): T =
        serializer.serialize(properties, templates, configType, component, environment)

    override fun toString(): String = declaringComponent.toString()
}
