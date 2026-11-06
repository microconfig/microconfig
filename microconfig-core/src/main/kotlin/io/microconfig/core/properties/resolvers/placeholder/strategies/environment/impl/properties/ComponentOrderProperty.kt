package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.Optional

class ComponentOrderProperty : EnvProperty {
    override fun key(): String = "order"

    override fun resolveFor(component: String, environment: Environment): Optional<String> =
        environment.findGroupWithComponent(component).flatMap { group ->
            val index = group.components.asList().indexOfFirst { it.name == component }
            Optional.ofNullable(index.takeIf { it >= 0 }?.toString())
        }
}
