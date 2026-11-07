package io.microconfig.core.environments.repository

import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.repository.impl.FileEnvironmentRepository
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import java.nio.file.Files
import java.nio.file.Path

class YamlEnvironmentRepositoryTest {
    @TempDir
    lateinit var root: Path

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
        writeEnvironment("dev.yaml", "{}\n")
        writeEnvironment("legacy.json", "invalid JSON and YAML: [")
        assertEquals(setOf("dev"), repository.environmentNames())
        assertEquals(
            listOf("dev"),
            repository.environments().stream().map { it.name }.toList(),
        )
    }

    @Test
    fun treatsJsonOnlyEnvironmentAsMissing() {
        writeEnvironment("legacy.json", "{\"profiles\": [\"json\"]}")
        assertThrows(EnvironmentException::class.java, { repository.getByName("legacy") })
        val environment: Environment = repository.getOrCreateByName("legacy")
        assertEquals("legacy", environment.name)
        assertNull(environment.source)
        assertTrue(environment.allComponents.asList().isEmpty())
    }

    @Test
    fun selectsYamlWhenJsonHasTheSameName() {
        val yaml: Path = writeEnvironment("dev.yaml", "profiles: [yaml]\n")
        writeEnvironment("dev.json", "{\"profiles\": [\"json\"]}")
        val environment: Environment = repository.getByName("dev")
        assertEquals(yaml.toFile(), environment.source)
        assertEquals(listOf("yaml"), environment.profiles)
        assertEquals(setOf("dev"), repository.environmentNames())
    }

    @Test
    fun cannotIncludeJsonEnvironment() {
        writeEnvironment("dev.yaml", "include:\n  env: legacy\n")
        writeEnvironment("legacy.json", "{\"profiles\": [\"json\"]}")
        val exception: EnvironmentException =
            assertThrows(EnvironmentException::class.java, { repository.getByName("dev") })
        assertTrue(exception.message.orEmpty().contains("Can\'t find env \'legacy\'"))
    }

    private fun writeEnvironment(name: String, content: String): Path {
        return Files.writeString(root.resolve("envs").resolve(name), content)
    }
}
