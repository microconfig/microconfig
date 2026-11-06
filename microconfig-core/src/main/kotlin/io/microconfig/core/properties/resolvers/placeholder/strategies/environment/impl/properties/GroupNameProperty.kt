package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.Optional

class GroupNameProperty : EnvProperty {
    override fun key(): String = "group"

    override fun resolveFor(component: String, environment: Environment): Optional<String> =
        environment.findGroupWithComponent(component).map { it.name }
}
