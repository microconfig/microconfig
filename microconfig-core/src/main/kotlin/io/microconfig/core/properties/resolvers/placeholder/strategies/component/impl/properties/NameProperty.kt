package io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.properties

import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty
import java.util.Optional

class NameProperty : ComponentProperty {
    override fun key(): String = "name"

    override fun resolveFor(component: String, environment: String): Optional<String> =
        Optional.of(component)
}
