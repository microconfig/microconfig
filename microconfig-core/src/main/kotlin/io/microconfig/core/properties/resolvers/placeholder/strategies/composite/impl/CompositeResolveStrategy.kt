package io.microconfig.core.properties.resolvers.placeholder.strategies.composite.impl

import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property
import java.util.Optional

class CompositeResolveStrategy(private val strategies: List<PlaceholderResolveStrategy>) :
    PlaceholderResolveStrategy {
    override fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property> =
        strategies
            .asSequence()
            .map { it.resolve(component, key, environment, configType, root) }
            .firstOrNull { it.isPresent } ?: Optional.empty()

    companion object {
        fun composite(strategies: List<PlaceholderResolveStrategy>): PlaceholderResolveStrategy =
            CompositeResolveStrategy(strategies)
    }
}
