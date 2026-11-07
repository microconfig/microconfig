package io.microconfig.core.environments

interface EnvironmentRepository {
    fun environments(): List<Environment>

    fun environmentNames(): Set<String>

    fun environmentNames(excluded: Set<String>): Set<String> = environmentNames() - excluded

    fun profilesOf(name: String): List<String> {
        val profiles = linkedSetOf<String>()
        collectProfiles(name, linkedSetOf(), profiles)
        return profiles.toList()
    }

    fun getByName(name: String): Environment

    fun getOrCreateByName(name: String): Environment

    private fun collectProfiles(
        name: String,
        visiting: MutableSet<String>,
        profiles: MutableSet<String>,
    ) {
        check(visiting.add(name)) {
            "Cyclic environment profiles: ${(visiting.toList() + name).joinToString(" -> ")}"
        }
        getOrCreateByName(name)
            .profiles
            .asSequence()
            .filterNot { it in profiles }
            .forEach { profile ->
                collectProfiles(profile, visiting, profiles)
                profiles.add(profile)
            }
        visiting.remove(name)
    }
}
