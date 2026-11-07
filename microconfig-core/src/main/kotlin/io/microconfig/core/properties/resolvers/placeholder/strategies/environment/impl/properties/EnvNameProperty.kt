package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.*

class EnvNameProperty : EnvProperty {
    override fun key(): String = "env"

    override fun resolveFor(component: String, environment: Environment): Optional<String> =
        Optional.of(environment.name)
}
