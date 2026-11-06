package io.microconfig.core.environments.impl

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.environments.Component
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertiesFactory

class ComponentImpl(
    private val configTypes: ConfigTypeRepository,
    private val properties: PropertiesFactory,
    override val name: String,
    override val originalName: String,
    override val environment: String,
) : Component {
    override fun getPropertiesFor(filter: ConfigTypeFilter): Properties =
        properties.getPropertiesOf(
            name,
            originalName,
            environment,
            filter.selectTypes(configTypes.configTypes),
        )

    override fun equals(other: Any?): Boolean =
        other is ComponentImpl &&
            name == other.name &&
            originalName == other.originalName &&
            environment == other.environment

    override fun hashCode(): Int = listOf(name, originalName, environment).hashCode()

    override fun toString(): String = name
}
