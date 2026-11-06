package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.Optional

class IpProperty : EnvProperty {
    override fun key(): String = "ip"

    override fun resolveFor(component: String, environment: Environment): Optional<String> =
        environment.findGroupWithComponent(component).flatMap { it.ip }
}
