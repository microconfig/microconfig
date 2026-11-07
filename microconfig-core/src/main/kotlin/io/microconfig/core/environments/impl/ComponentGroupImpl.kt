package io.microconfig.core.environments.impl

import io.microconfig.core.environments.Component
import io.microconfig.core.environments.ComponentGroup
import io.microconfig.core.environments.Components
import java.util.Optional

class ComponentGroupImpl(
    override val name: String,
    override val components: Components,
) : ComponentGroup {
    override fun findComponentWithName(name: String): Optional<Component> =
        Optional.ofNullable(components.asList().firstOrNull { it.name == name })

    override fun toString(): String = "$name: $components"
}
