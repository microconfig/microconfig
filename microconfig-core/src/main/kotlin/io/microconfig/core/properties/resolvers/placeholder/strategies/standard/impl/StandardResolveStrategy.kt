package io.microconfig.core.properties.resolvers.placeholder.strategies.standard.impl

import io.microconfig.core.configtypes.ConfigTypeFilters.configTypeWithName
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.Property
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StandardResolveStrategy(private val environments: EnvironmentRepository) :
    PlaceholderResolveStrategy {
    private val properties = ConcurrentHashMap<Triple<String, String, String>, Properties>()

    override fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property> =
        properties.getOrPut(Triple(component, environment, configType)) {
            readProperties(component, environment, configType)
        }.getPropertyWithKey(key)

    private fun readProperties(component: String, environment: String, configType: String): Properties =
        environments.getOrCreateByName(environment)
            .findComponentWithName(component)
            .getPropertiesFor(configTypeWithName(configType))
}
