package io.microconfig.core.environments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class EnvironmentRepositoryTest {
    @Test
    fun traverseSharedProfilesOnceInDependencyOrder() {
        val parents =
            (0 until 25).associate { index ->
                "p$index" to
                        if (index < 2) emptyList() else listOf("p${index - 1}", "p${index - 2}")
            }
        val lookups = mutableMapOf<String, Int>()
        val repository = repository(parents, lookups)
        val expected = listOf("p1", "p0") + (2 until 24).map { "p$it" }
        assertEquals(expected, repository.profilesOf("p24"))
        assertEquals(parents.keys.associateWith { 1 }, lookups)
    }

    @Test
    fun detectCyclesWhileSkippingCompletedProfiles() {
        val repository =
            repository(
                mapOf("root" to listOf("done", "a"), "done" to emptyList(), "a" to listOf("root")),
                mutableMapOf(),
            )
        assertThrows(IllegalStateException::class.java) { repository.profilesOf("root") }
    }

    private fun repository(
        parents: Map<String, List<String>>,
        lookups: MutableMap<String, Int>,
    ): EnvironmentRepository {
        val environments = parents.mapValues { (_, inheritedProfiles) ->
            object : Environment by mock(Environment::class.java) {
                override val profiles = inheritedProfiles
            }
        }
        return object : EnvironmentRepository {
            override fun environments() = environments.values.toList()

            override fun environmentNames() = environments.keys

            override fun getByName(name: String): Environment {
                lookups[name] = lookups.getOrDefault(name, 0) + 1
                return environments.getValue(name)
            }

            override fun getOrCreateByName(name: String) = getByName(name)
        }
    }
}
