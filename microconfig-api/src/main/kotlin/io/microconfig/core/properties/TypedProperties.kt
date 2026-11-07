package io.microconfig.core.properties

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.templates.Template
import java.util.*
import java.util.function.Predicate

interface TypedProperties {
    val configType: ConfigType
    val declaringComponent: DeclaringComponent
    val propertiesAsMap: Map<String, Property>
    val propertiesAsKeyValue: Map<String, String>
    val properties: Collection<Property>
    val templates: List<Template>
        get() = emptyList()

    fun resolveBy(resolver: Resolver): TypedProperties

    fun withoutVars(): TypedProperties

    fun without(excluded: Predicate<Property>): TypedProperties

    fun withPrefix(prefix: String): TypedProperties

    fun withTemplates(templates: List<Template>): TypedProperties

    fun getPropertyWithKey(key: String): Optional<Property>

    fun <T> save(serializer: PropertySerializer<T>): T
}
