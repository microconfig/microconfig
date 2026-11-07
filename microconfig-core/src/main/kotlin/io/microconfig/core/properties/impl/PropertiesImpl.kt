package io.microconfig.core.properties.impl

import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import java.util.Optional
import java.util.function.Predicate
import java.util.function.UnaryOperator

data class PropertiesImpl(private val typedProperties: List<TypedProperties>) : Properties {
    override val propertiesAsMap: Map<String, Property>
        get() = propertyKeyTo { it }

    override val propertiesAsKeyValue: Map<String, String>
        get() = propertyKeyTo { it.value }

    override val properties: Collection<Property>
        get() = typedProperties.flatMap { it.properties }

    private fun <T> propertyKeyTo(value: (Property) -> T): Map<String, T> = buildMap {
        properties.forEach { property ->
            check(property.key !in this) { "Duplicate key ${property.key}" }
            put(property.key, value(property))
        }
    }

    override fun resolveBy(resolver: Resolver): Properties = withEachComponent {
        it.resolveBy(resolver)
    }

    private fun withEachComponent(callback: (TypedProperties) -> TypedProperties): Properties =
        PropertiesImpl(typedProperties.parallelStream().map(callback).toList())

    override fun withoutVars(): Properties = withEachComponent { it.withoutVars() }

    override fun without(excluded: Predicate<Property>): Properties = withEachComponent {
        it.without(excluded)
    }

    override fun withPrefix(prefix: String): Properties = withEachComponent {
        it.withPrefix(prefix)
    }

    override fun getPropertyWithKey(key: String): Optional<Property> =
        typedProperties.asSequence().map { it.getPropertyWithKey(key) }.firstOrNull { it.isPresent }
            ?: Optional.empty()

    override fun <T> save(serializer: PropertySerializer<T>): List<T> {
        val prepared = serializer.prepare(typedProperties)
        return typedProperties.map { it.save(prepared) }
    }

    override fun asTypedProperties(): List<TypedProperties> = typedProperties

    override fun forEachComponent(callback: UnaryOperator<TypedProperties>): Properties =
        withEachComponent(callback::apply)

    override fun first(): TypedProperties = typedProperties.first()

    companion object {
        fun flat(properties: List<Properties>): Properties =
            PropertiesImpl(properties.flatMap { it.asTypedProperties() })
    }
}
