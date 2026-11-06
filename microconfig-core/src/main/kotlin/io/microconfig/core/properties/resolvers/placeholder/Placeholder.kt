package io.microconfig.core.properties.resolvers.placeholder

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property

class Placeholder
internal constructor(
    val configType: String,
    val component: String,
    val environment: String,
    val key: String,
    val defaultValue: String?,
) {
    val referencedComponent: DeclaringComponent
        get() = DeclaringComponentImpl(configType, component, environment)

    val isSelfReferenced: Boolean
        get() = component == "this"

    fun resolveUsing(strategy: PlaceholderResolveStrategy, root: DeclaringComponent): Property =
        strategy.resolve(component, key, environment, configType, root.component).orElseThrow {
            IllegalStateException("Can't resolve $this")
        }

    fun referencedTo(source: DeclaringComponent): Boolean =
        component == source.component && environment == source.environment

    fun overrideBy(source: DeclaringComponent): Placeholder =
        Placeholder(configType, source.component, source.environment, key, defaultValue)

    fun withEnvironment(environment: String): Placeholder =
        Placeholder(configType, component, environment, key, defaultValue)

    fun withComponent(component: String): Placeholder =
        Placeholder(configType, component, environment, key, defaultValue)

    override fun equals(other: Any?): Boolean =
        other is Placeholder &&
            configType == other.configType &&
            component == other.component &&
            environment == other.environment &&
            key == other.key

    override fun hashCode(): Int = listOf(configType, component, environment, key).hashCode()

    override fun toString(): String =
        "\${$component[$environment]@$key${defaultValue?.let { ":$it" } ?: ""}}"
}
