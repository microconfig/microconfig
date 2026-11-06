package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.Component
import io.microconfig.core.environments.ComponentFactory

internal data class ComponentDefinition(val name: String, private val originalName: String) {
    fun toComponent(factory: ComponentFactory, environment: String): Component =
        factory.createComponent(name, originalName, environment)

    companion object {
        fun withAlias(alias: String, originalName: String): ComponentDefinition =
            ComponentDefinition(alias, originalName)

        fun withName(name: String): ComponentDefinition = withAlias(name, name)
    }
}
