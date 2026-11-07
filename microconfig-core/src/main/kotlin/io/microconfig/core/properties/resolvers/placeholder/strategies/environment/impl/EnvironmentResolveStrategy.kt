package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl

import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class EnvironmentResolveStrategy(
    private val environments: EnvironmentRepository,
    private val properties: Map<String, EnvProperty>,
) : PlaceholderResolveStrategy {
    override fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property> = Optional.ofNullable(resolveProperty(component, key, environment, configType))

    private fun resolveProperty(
        component: String,
        key: String,
        environment: String,
        configType: String,
    ): Property? {
        val property = properties[key] ?: return null
        val context = findEnvironment(environment) ?: return null
        val value = property.resolveFor(component, context).getOrNull() ?: return null
        return PropertyImpl.property(
            key,
            value,
            PROPERTIES,
            DeclaringComponentImpl(configType, component, environment),
        )
    }

    private fun findEnvironment(name: String): Environment? =
        try {
            environments.getByName(name)
        } catch (_: EnvironmentException) {
            null
        }
}
