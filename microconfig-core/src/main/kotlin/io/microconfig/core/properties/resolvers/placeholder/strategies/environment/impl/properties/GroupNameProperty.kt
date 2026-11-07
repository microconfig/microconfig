package io.microconfig.core.properties.resolvers.placeholder.strategies.environment.impl.properties

import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class GroupNameProperty : EnvProperty {
    override fun key(): String = "group"

    override fun resolveFor(component: String, environment: Environment): Optional<String> =
        Optional.ofNullable(environment.findGroupWithComponent(component).getOrNull()?.name)
}
