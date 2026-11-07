package io.microconfig.core.environments.repository

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.environments.repository.impl.LazyInitEnvRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LazyInitEnvRepositoryTest {
    @Test
    fun initializeOnceAtFirstAccessAndUseTheSameRepositoryForAllMethods() {
        val delegate = mock(EnvironmentRepository::class.java)
        `when`(delegate.environmentNames()).thenReturn(setOf("dev"))
        `when`(delegate.environmentNames(setOf("dev"))).thenReturn(emptySet())
        var initializations = 0
        val repository = LazyInitEnvRepository { initializations++; delegate }
        assertEquals(0, initializations)

        assertEquals(setOf("dev"), repository.environmentNames())
        assertEquals(emptySet<String>(), repository.environmentNames(setOf("dev")))
        repository.environments()
        repository.profilesOf("dev")
        repository.getByName("dev")
        repository.getOrCreateByName("missing")

        assertEquals(1, initializations)
    }
}
