package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class ComponentOrderProperty : EnvProperty {
    override fun key(): String = "order"

    override fun resolveFor(component: String, environment: Environment): Optional<String> =
        Optional.ofNullable(environment.findGroupWithComponent(component).getOrNull()?.let { group ->
            val index = group.components.asList().indexOfFirst { it.name == component }
            index.takeIf { it >= 0 }?.toString()
        })
}
