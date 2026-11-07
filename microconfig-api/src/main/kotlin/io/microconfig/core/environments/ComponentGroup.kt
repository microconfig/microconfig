package io.microconfig.core.environments

import java.util.*

interface ComponentGroup {
    val name: String
    val components: Components

    fun findComponentWithName(name: String): Optional<Component>
}
