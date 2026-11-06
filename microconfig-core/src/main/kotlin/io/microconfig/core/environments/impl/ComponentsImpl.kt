package io.microconfig.core.environments.impl

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.environments.Component
import io.microconfig.core.environments.Components
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertiesFactory

data class ComponentsImpl(
    private val components: List<Component>,
    private val properties: PropertiesFactory,
) : Components {
    override fun asList(): List<Component> = components

    override fun getPropertiesFor(filter: ConfigTypeFilter): Properties =
        properties.flat(components.parallelStream().map { it.getPropertiesFor(filter) }.toList())

    override fun toString(): String = components.toString()
}
