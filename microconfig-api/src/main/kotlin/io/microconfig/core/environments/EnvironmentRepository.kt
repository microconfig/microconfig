package io.microconfig.core.environments

interface EnvironmentRepository {
    fun environments(): List<Environment>

    fun environmentNames(): Set<String>

    fun environmentNames(excluded: Set<String>): Set<String> = environmentNames() - excluded

    fun profilesOf(name: String): List<String> = collectProfiles(name, emptyList()).distinct()

    private fun collectProfiles(name: String, visiting: List<String>): List<String> {
        check(name !in visiting) {
            "Cyclic environment profiles: ${(visiting + name).joinToString(" -> ")}"
        }
        return getOrCreateByName(name).profiles.flatMap {
            collectProfiles(it, visiting + name) + it
        }
    }

    fun getByName(name: String): Environment

    fun getOrCreateByName(name: String): Environment
}
