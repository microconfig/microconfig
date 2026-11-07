package io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class ComponentResolveStrategy(private val properties: Map<String, ComponentProperty>) :
    PlaceholderResolveStrategy {
    override fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property> =
        Optional.ofNullable(properties[key]?.resolveFor(component, environment)?.getOrNull()?.let { value ->
            PropertyImpl.property(
                component,
                value,
                PROPERTIES,
                DeclaringComponentImpl(configType, component, environment),
            )
        })
}
