package io.microconfig.core.environments

import java.util.Optional

interface ComponentGroup {
    val name: String
    val components: Components

    fun findComponentWithName(name: String): Optional<Component>
}
