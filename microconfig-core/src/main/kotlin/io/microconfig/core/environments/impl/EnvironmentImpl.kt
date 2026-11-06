package io.microconfig.core.environments.impl

import io.microconfig.core.environments.Component
import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.ComponentGroup
import io.microconfig.core.environments.Components
import io.microconfig.core.environments.Environment
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.core.properties.repository.ComponentNotFoundException
import io.microconfig.utils.Logger.info
import java.io.File
import java.util.Optional

class EnvironmentImpl(
    override val source: File?,
    override val name: String,
    override val isAbstract: Boolean,
    override val portOffset: Int,
    override val profiles: List<String>,
    override val groups: List<ComponentGroup>,
    private val componentFactory: ComponentFactory,
    private val propertiesFactory: PropertiesFactory,
) : Environment {
    override val allComponents: Components
        get() = ComponentsImpl(groups.flatMap { it.components.asList() }, propertiesFactory)

    override fun findGroupsWithIp(ip: String): List<ComponentGroup> = groups.filter {
        it.ip.orElse(null) == ip
    }

    override fun getGroupWithName(groupName: String): ComponentGroup =
        groups.firstOrNull { it.name == groupName }
            ?: throw IllegalArgumentException(
                "Can't find group by filter: 'groupName=$groupName' in env '$name'"
            )

    override fun findGroupWithComponent(componentName: String): Optional<ComponentGroup> =
        Optional.ofNullable(
            groups.firstOrNull { it.findComponentWithName(componentName).isPresent }
        )

    override fun getComponentWithName(componentName: String): Component =
        findGroupWithComponent(componentName)
            .flatMap { it.findComponentWithName(componentName) }
            .orElseThrow { ComponentNotFoundException(componentName) }

    override fun findComponentsFrom(groups: List<String>, components: List<String>): Components {
        val available = componentsFrom(groups)
        val selected = selectComponents(components, available)
        info("Filtered ${selected.size} component(s) in [$name] env.")
        return ComponentsImpl(selected, propertiesFactory)
    }

    private fun componentsFrom(groups: List<String>): List<Component> =
        if (groups.isEmpty()) allComponents.asList()
        else groups.flatMap { getGroupWithName(it).components.asList() }

    private fun selectComponents(names: List<String>, available: List<Component>): List<Component> {
        if (names.isEmpty()) return available
        val components = available.associateBy { it.name }
        check(components.size == available.size) { "Duplicate component declarations" }
        return names.map { components[it] ?: throw ComponentNotFoundException(it) }
    }

    override fun findComponentWithName(componentName: String): Component =
        findGroupWithComponent(componentName)
            .flatMap { it.findComponentWithName(componentName) }
            .orElseGet { componentFactory.createComponent(componentName, componentName, name) }

    override fun toString(): String = "$name: $groups"
}
