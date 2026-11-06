package io.microconfig.core.properties.resolvers.placeholder.strategies.component

import java.util.Optional

interface ComponentProperty {
    fun key(): String

    fun resolveFor(component: String, environment: String): Optional<String>
}
