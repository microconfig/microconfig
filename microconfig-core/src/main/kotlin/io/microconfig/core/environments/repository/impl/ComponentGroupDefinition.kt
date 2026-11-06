package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.ComponentGroup
import io.microconfig.core.environments.impl.ComponentGroupImpl
import io.microconfig.core.environments.impl.ComponentsImpl
import io.microconfig.core.properties.PropertiesFactory

internal data class ComponentGroupDefinition(
    val name: String,
    private val ip: String?,
    val components: List<ComponentDefinition>,
    private val excludedComponents: List<ComponentDefinition>,
    private val appendedComponents: List<ComponentDefinition>,
) {
    fun overrideBy(override: ComponentGroupDefinition): ComponentGroupDefinition =
        withOverrides(override).applyComponentChanges()

    private fun withOverrides(override: ComponentGroupDefinition): ComponentGroupDefinition =
        copy(
            ip = override.ip ?: ip,
            components = override.components.ifEmpty { components },
            excludedComponents = override.excludedComponents,
            appendedComponents = override.appendedComponents,
        )

    fun applyComponentChanges(): ComponentGroupDefinition =
        excluding(excludedComponents).appending(appendedComponents)

    private fun excluding(excluded: List<ComponentDefinition>): ComponentGroupDefinition {
        if (excluded.isEmpty()) return this
        val names = excluded.map { it.name }.toSet()
        return copy(
            components = components.filterNot { it.name in names },
            excludedComponents = emptyList(),
        )
    }

    private fun appending(appended: List<ComponentDefinition>): ComponentGroupDefinition =
        if (appended.isEmpty()) this
        else copy(components = components + appended, appendedComponents = emptyList())

    fun toGroup(
        factory: ComponentFactory,
        properties: PropertiesFactory,
        environment: String,
    ): ComponentGroup =
        ComponentGroupImpl(
            name,
            ip,
            ComponentsImpl(components.map { it.toComponent(factory, environment) }, properties),
        )
}
