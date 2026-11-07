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
import kotlin.jvm.optionals.getOrNull

class EnvironmentImpl(
    override val source: File?,
    override val name: String,
    override val isAbstract: Boolean,
    override val profiles: List<String>,
    override val groups: List<ComponentGroup>,
    private val componentFactory: ComponentFactory,
    private val propertiesFactory: PropertiesFactory,
) : Environment {
    override val allComponents: Components
        get() = ComponentsImpl(groups.flatMap { it.components.asList() }, propertiesFactory)

    override fun getGroupWithName(groupName: String): ComponentGroup =
        groups.firstOrNull { it.name == groupName }
            ?: throw IllegalArgumentException(
                "Can't find group by filter: 'groupName=$groupName' in env '$name'"
            )

    override fun findGroupWithComponent(componentName: String): Optional<ComponentGroup> =
        Optional.ofNullable(groupWithComponent(componentName))

    private fun groupWithComponent(componentName: String): ComponentGroup? =
        groups.firstOrNull { it.findComponentWithName(componentName).getOrNull() != null }

    override fun getComponentWithName(componentName: String): Component =
        componentWithName(componentName) ?: throw ComponentNotFoundException(componentName)

    private fun componentWithName(componentName: String): Component? =
        groups.firstNotNullOfOrNull { it.findComponentWithName(componentName).getOrNull() }

    override fun findComponentsFrom(groups: List<String>, components: List<String>): Components {
        val available = componentsFrom(groups)
        val selected = selectComponents(components, available)
        info("Filtered ${selected.size} component(s) in [$name] env.")
        return ComponentsImpl(selected, propertiesFactory)
    }

    private fun componentsFrom(groups: List<String>): List<Component> =
        if (groups.isEmpty()) allComponents.asList()
        else groups.distinct().flatMap { getGroupWithName(it).components.asList() }

    private fun selectComponents(names: List<String>, available: List<Component>): List<Component> {
        val byName = available.associateBy { it.name }
        check(byName.size == available.size) {
            val duplicates =
                available.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
            "Duplicate component declarations in '$name' env: $duplicates"
        }
        if (names.isEmpty()) return available
        return names.map { byName[it] ?: throw ComponentNotFoundException(it) }
    }

    override fun findComponentWithName(componentName: String): Component =
        componentWithName(componentName)
            ?: componentFactory.createComponent(componentName, componentName, name)

    override fun toString(): String = "$name: $groups"
}
