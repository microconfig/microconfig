package io.microconfig.core.environments.repository.impl

internal class EnvInclude(
    private val baseEnvironment: String,
    private val excludedGroups: Set<String>,
) {
    fun includeTo(
        destination: EnvironmentDefinition,
        repository: (String) -> EnvironmentDefinition,
    ): EnvironmentDefinition {
        val groups = inheritedGroups(destination, repository)
        overrideGroups(groups, destination.groups)
        return destination.copy(groups = groups.values.toList(), envInclude = empty())
    }

    private fun inheritedGroups(
        destination: EnvironmentDefinition,
        repository: (String) -> EnvironmentDefinition,
    ): MutableMap<String, ComponentGroupDefinition> =
        repository(baseEnvironment)
            .groups
            .filterNot { it.name in excludedGroups }
            .map { if (destination.ip == null) it else it.copy(ip = destination.ip) }
            .associateBy { it.name }
            .toMutableMap()

    private fun overrideGroups(
        groups: MutableMap<String, ComponentGroupDefinition>,
        overrides: List<ComponentGroupDefinition>,
    ) {
        overrides.forEach { group ->
            groups[group.name] = groups[group.name]?.overrideBy(group) ?: group
        }
    }

    fun isEmpty(): Boolean = baseEnvironment.isEmpty()

    companion object {
        fun empty(): EnvInclude = EnvInclude("", emptySet())
    }
}
