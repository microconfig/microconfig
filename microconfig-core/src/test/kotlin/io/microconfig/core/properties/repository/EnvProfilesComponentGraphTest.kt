package io.microconfig.core.properties.repository

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.repository.impl.EnvProfilesComponentGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.io.File

class EnvProfilesComponentGraphTest {
    val delegate: ComponentGraph = mock(ComponentGraph::class.java)

    val environmentRepository: EnvironmentRepository = mock(EnvironmentRepository::class.java)

    val e: Environment = mock(Environment::class.java)

    val p: Environment = mock(Environment::class.java)

    val componentGraph: ComponentGraph = EnvProfilesComponentGraph(delegate, environmentRepository)

    @BeforeEach
    fun setUp() {
        `when`(environmentRepository.getOrCreateByName("e")).thenReturn(e)
        `when`(environmentRepository.getOrCreateByName("p")).thenReturn(p)
        `when`(e.profiles).thenReturn(listOf("p"))
        `when`(p.profiles).thenReturn(emptyList())
    }

    @Test
    fun test() {
        mockGraph("e", "app.yaml", "app.e.yaml")
        mockGraph("p", "app.yaml", "app.p.yaml")
        assertResult("app.yaml", "app.p.yaml", "app.e.yaml")
    }

    @Test
    fun test2() {
        mockGraph("e", "app.yaml", "app.e.p.yaml")
        mockGraph("p", "app.yaml", "app.e.p.yaml")
        assertResult("app.yaml", "app.e.p.yaml")
    }

    @Test
    fun test3() {
        mockGraph("e", "app.yaml", "app.e.e2.e3.p.yaml", "app.e.e2.e3.yaml", "app.e.yaml")
        mockGraph("p", "app.yaml", "app.p.yaml", "app.e.e2.e3.p.yaml")
        assertResult(
            "app.yaml",
            "app.p.yaml",
            "app.e.e2.e3.p.yaml",
            "app.e.e2.e3.yaml",
            "app.e.yaml",
        )
    }

    @Test
    fun test4() {
        mockGraph("e", "app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.yaml")
        mockGraph("p", "app.yaml", "app.e.p.yaml")
        assertResult("app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.yaml")
    }

    @Test
    fun test5() {
        `when`(p.profiles).thenReturn(listOf("z"))
        val z: Environment = mock(Environment::class.java)
        `when`(environmentRepository.getOrCreateByName("z")).thenReturn(z)
        mockGraph("e", "app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.z.yaml", "app.e.yaml")
        mockGraph("p", "app.yaml", "app.e.p.yaml")
        mockGraph("z", "app.yaml", "app.e.z.yaml")
        assertResult("app.yaml", "app.e.e2.e3.yaml", "app.e.p.yaml", "app.e.z.yaml", "app.e.yaml")
    }

    @Test
    fun test6() {
        `when`(e.profiles).thenReturn(listOf("p", "z"))
        val z: Environment = mock(Environment::class.java)
        `when`(environmentRepository.getOrCreateByName("z")).thenReturn(z)
        mockGraph("e", "app.yaml", "app.e.yaml")
        mockGraph("p", "app.yaml", "app.p.z.yaml", "app.p.yaml")
        mockGraph("z", "app.yaml", "app.p.z.yaml")
        assertResult("app.yaml", "app.p.z.yaml", "app.p.yaml", "app.e.yaml")
    }

    private fun mockGraph(env: String, vararg files: String) {
        `when`(delegate.getConfigFilesOf("c", env, APPLICATION)).thenReturn(files(*files))
    }

    @Test
    fun visitSharedProfileFilesOnce() {
        val shared = mock(Environment::class.java)
        val sibling = mock(Environment::class.java)
        `when`(e.profiles).thenReturn(listOf("p", "sibling"))
        `when`(p.profiles).thenReturn(listOf("shared"))
        `when`(sibling.profiles).thenReturn(listOf("shared"))
        `when`(shared.profiles).thenReturn(emptyList())
        `when`(environmentRepository.getOrCreateByName("sibling")).thenReturn(sibling)
        `when`(environmentRepository.getOrCreateByName("shared")).thenReturn(shared)
        listOf("e", "p", "sibling", "shared").forEach { mockGraph(it, "app.yaml") }
        assertResult("app.yaml")
        verify(delegate, times(1)).getConfigFilesOf("c", "shared", APPLICATION)
    }

    private fun assertResult(vararg names: String) {
        assertEquals(files(*names), componentGraph.getConfigFilesOf("c", "e", APPLICATION))
    }

    private fun files(vararg names: String): List<ConfigFile> {
        return names.map { ConfigFile(File(it), "ct1", "e") }
    }
}
