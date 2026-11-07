package io.microconfig.core.properties

import java.util.*
import java.util.function.Predicate
import java.util.function.UnaryOperator

interface Properties {
    val propertiesAsMap: Map<String, Property>
    val propertiesAsKeyValue: Map<String, String>
    val properties: Collection<Property>

    fun resolveBy(resolver: Resolver): Properties

    fun withoutVars(): Properties

    fun without(excluded: Predicate<Property>): Properties

    fun withPrefix(prefix: String): Properties

    fun getPropertyWithKey(key: String): Optional<Property>

    fun <T> save(serializer: PropertySerializer<T>): List<T>

    fun asTypedProperties(): List<TypedProperties>

    fun forEachComponent(callback: UnaryOperator<TypedProperties>): Properties

    fun first(): TypedProperties
}
