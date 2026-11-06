package io.microconfig.core.environments.repository

import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.repository.impl.FileEnvironmentRepository
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.io.impl.DumpedFsReader
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock

class YamlEnvironmentRepositoryTest {
    @TempDir lateinit var root: Path

    lateinit var repository: FileEnvironmentRepository

    @BeforeEach
    fun setUp() {
        Files.createDirectories(root.resolve("envs"))
        repository =
            FileEnvironmentRepository(
                root.toFile(),
                DumpedFsReader(),
                mock(ComponentFactory::class.java),
                mock(PropertiesFactory::class.java),
            )
    }

    @Test
    fun ignoresJsonWhenListingEnvironments() {
        writeEnvironment("dev.yaml", "portOffset: 7\n")
        writeEnvironment("legacy.json", "invalid JSON and YAML: [")
        assertEquals(setOf("dev"), repository.environmentNames())
        assertEquals(
            listOf("dev"),
            repository.environments().stream().map(Environment::name).toList(),
        )
    }

    @Test
    fun treatsJsonOnlyEnvironmentAsMissing() {
        writeEnvironment("legacy.json", "{\"portOffset\": 99}")
        assertThrows(EnvironmentException::class.java, { repository.getByName("legacy") })
        val environment: Environment = repository.getOrCreateByName("legacy")
        assertEquals("legacy", environment.name)
        assertNull(environment.source)
        assertEquals(0, environment.portOffset)
        assertTrue(environment.allComponents.asList().isEmpty())
    }

    @Test
    fun selectsYamlWhenJsonHasTheSameName() {
        val yaml: Path = writeEnvironment("dev.yaml", "portOffset: 7\n")
        writeEnvironment("dev.json", "{\"portOffset\": 99}")
        val environment: Environment = repository.getByName("dev")
        assertEquals(yaml.toFile(), environment.source)
        assertEquals(7, environment.portOffset)
        assertEquals(setOf("dev"), repository.environmentNames())
    }

    @Test
    fun cannotIncludeJsonEnvironment() {
        writeEnvironment("dev.yaml", "include:\n  env: legacy\n")
        writeEnvironment("legacy.json", "{\"portOffset\": 99}")
        val exception: EnvironmentException =
            assertThrows(EnvironmentException::class.java, { repository.getByName("dev") })
        assertTrue(exception.message.orEmpty().contains("Can\'t find env \'legacy\'"))
    }

    private fun writeEnvironment(name: String, content: String): Path {
        return Files.writeString(root.resolve("envs").resolve(name), content)
    }
}
