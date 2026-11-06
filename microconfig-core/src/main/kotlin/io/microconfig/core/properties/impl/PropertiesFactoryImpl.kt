package io.microconfig.core.properties.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.name
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.TypedProperties

class PropertiesFactoryImpl(private val repository: PropertiesRepository) : PropertiesFactory {
    override fun getPropertiesOf(
        componentName: String,
        componentOriginalName: String,
        environment: String,
        configTypes: List<ConfigType>,
    ): Properties =
        PropertiesImpl(
            configTypes.map { type ->
                readProperties(componentName, componentOriginalName, environment, type)
            }
        )

    private fun readProperties(
        name: String,
        originalName: String,
        environment: String,
        type: ConfigType,
    ): TypedProperties =
        TypedPropertiesImpl(
            type,
            name,
            environment,
            repository.getPropertiesOf(originalName, environment, type),
            emptyList(),
        )

    override fun flat(properties: List<Properties>): Properties = PropertiesImpl.flat(properties)
}
