package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty

class EnvironmentProperties {
    fun get(): Map<String, EnvProperty> =
        listOf(
                EnvNameProperty(),
                IpProperty(),
                GroupNameProperty(),
                ComponentOrderProperty(),
                PortOffsetProperty(),
            )
            .associateBy { it.key() }
}
