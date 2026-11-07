package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.impl.EnvironmentImpl
import io.microconfig.core.properties.PropertiesFactory
import java.io.File

internal data class EnvironmentDefinition(
    private val source: File,
    private val name: String,
    private val abstractEnv: Boolean,
    private val profiles: List<String>,
    private val envInclude: EnvInclude,
    val groups: List<ComponentGroupDefinition>,
) {
    fun processIncludeUsing(repository: (String) -> EnvironmentDefinition): EnvironmentDefinition {
        val environment = if (envInclude.isEmpty()) this else envInclude.includeTo(this, repository)
        return environment.applyComponentChanges()
    }

    private fun applyComponentChanges(): EnvironmentDefinition =
        copy(groups = groups.map { it.applyComponentChanges() })

    fun checkComponentNamesAreUnique(): EnvironmentDefinition {
        val duplicates =
            groups
                .flatMap { it.components }
                .groupingBy { it.name }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        check(duplicates.isEmpty()) {
            "Environment '$name' contains several declarations of: $duplicates"
        }
        return this
    }

    fun toEnvironment(factory: ComponentFactory, properties: PropertiesFactory): Environment =
        EnvironmentImpl(
            source,
            name,
            abstractEnv,
            profiles,
            groups.map { it.toGroup(factory, properties, name) },
            factory,
            properties,
        )
}
