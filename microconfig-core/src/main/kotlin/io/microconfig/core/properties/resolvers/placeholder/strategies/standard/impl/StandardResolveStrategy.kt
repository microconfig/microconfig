package io.microconfig.core.properties.resolvers.placeholder.strategies.standard.impl

import io.microconfig.core.configtypes.ConfigTypeFilters.configTypeWithName
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property
import java.util.Optional

class StandardResolveStrategy(private val environments: EnvironmentRepository) :
    PlaceholderResolveStrategy {
    override fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property> =
        environments
            .getOrCreateByName(environment)
            .findComponentWithName(component)
            .getPropertiesFor(configTypeWithName(configType))
            .getPropertyWithKey(key)
}
