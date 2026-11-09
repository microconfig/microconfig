package io.microconfig.core.properties.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.properties.*
import java.util.concurrent.ConcurrentHashMap

/** Concurrent cache misses may read in parallel; callers share the published repository snapshot. */
class PropertiesFactoryImpl(private val repository: PropertiesRepository) : PropertiesFactory {
    private val properties = ConcurrentHashMap<Query, Map<String, Property>>()

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

    override fun flat(properties: List<Properties>): Properties = PropertiesImpl.flat(properties)

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
            originalProperties(originalName, environment, type),
            emptyList(),
        )

    private fun originalProperties(component: String, environment: String, type: ConfigType): Map<String, Property> {
        val query = Query(component, environment, type)
        // Repository callbacks may request other keys from this factory.
        return properties.getOrPut(query) { repository.getPropertiesOf(component, environment, type) }
    }

    private data class Query(val component: String, val environment: String, val type: ConfigType)
}
