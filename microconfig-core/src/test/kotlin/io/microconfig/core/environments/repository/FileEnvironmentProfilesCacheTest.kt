package io.microconfig.core.environments.repository

import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.repository.impl.FileEnvironmentRepository
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.io.impl.DumpedFsReader
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class FileEnvironmentProfilesCacheTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironmentDirectory() {
        Files.createDirectories(root.resolve("envs"))
    }

    @Test
    fun reuseTheOrderedTransitiveProfilesWithoutRepeatingLookups() {
        writeProfiles("dev", "first", "second")
        writeProfiles("first", "shared")
        writeProfiles("second", "shared")
        writeProfiles("shared")
        val repository = repository()

        assertEquals(listOf("shared", "first", "second"), repository.profilesOf("dev"))
        assertEquals(listOf("shared", "first", "second"), repository.profilesOf("dev"))

        for (name in listOf("dev", "first", "second", "shared"))
            verify(repository, times(1)).getOrCreateByName(name)
    }

    @Test
    fun keepTheCacheAndProfileOrderSeparateForEachEnvironment() {
        writeProfiles("dev", "first", "second")
        writeProfiles("prod", "second", "first")
        writeProfiles("first")
        writeProfiles("second")
        val repository = repository()

        repeat(2) {
            assertEquals(listOf("first", "second"), repository.profilesOf("dev"))
            assertEquals(listOf("second", "first"), repository.profilesOf("prod"))
        }

        verify(repository, times(1)).getOrCreateByName("dev")
        verify(repository, times(1)).getOrCreateByName("prod")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun cacheEmptyProfilesForExistingAndMissingEnvironments(existing: Boolean) {
        if (existing) writeProfiles("empty")
        val repository = repository()

        assertEquals(emptyList<String>(), repository.profilesOf("empty"))
        assertEquals(emptyList<String>(), repository.profilesOf("empty"))

        verify(repository, times(1)).getOrCreateByName("empty")
    }

    @Test
    fun retryFailedCyclesWithoutCachingAPartialProfileList() {
        writeProfiles("dev", "base")
        writeProfiles("base", "dev")
        val repository = repository()

        repeat(2) {
            val error = assertThrows(IllegalStateException::class.java) { repository.profilesOf("dev") }
            assertEquals("Cyclic environment profiles: dev -> base -> dev", error.message)
        }

        verify(repository, times(2)).getOrCreateByName("dev")
        verify(repository, times(2)).getOrCreateByName("base")
    }

    @Test
    fun preventMutationsOfReturnedListsFromChangingTheCachedProfiles() {
        writeProfiles("dev", "first", "second", "third")
        val repository = repository()
        val profiles = repository.profilesOf("dev")

        (profiles as MutableList<String>).clear()

        assertEquals(listOf("first", "second", "third"), repository.profilesOf("dev"))
        verify(repository, times(1)).getOrCreateByName("dev")
    }

    @Test
    fun letANewRepositoryReadUpdatedProfiles() {
        writeProfiles("dev", "first")
        val original = repository()
        assertEquals(listOf("first"), original.profilesOf("dev"))

        writeProfiles("dev", "second")

        assertEquals(listOf("first"), original.profilesOf("dev"))
        assertEquals(listOf("second"), repository().profilesOf("dev"))
    }

    private fun writeProfiles(name: String, vararg profiles: String) {
        Files.writeString(root.resolve("envs/$name.yaml"),
            if (profiles.isEmpty()) "{}\n" else "profiles: [${profiles.joinToString(", ")}]\n")
    }

    private fun repository(): FileEnvironmentRepository =
        spy(FileEnvironmentRepository(root.toFile(), DumpedFsReader(),
            mock(ComponentFactory::class.java), mock(PropertiesFactory::class.java)))
}
