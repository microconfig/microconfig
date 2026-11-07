package io.microconfig.core.properties.resolvers.placeholder.strategies.environment

import io.microconfig.core.environments.Environment
import java.util.*

interface EnvProperty {
    fun key(): String

    fun resolveFor(component: String, environment: Environment): Optional<String>
}
