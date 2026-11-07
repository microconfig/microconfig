package io.microconfig.core

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DependencyInitializationTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "profiles: [base]\ngroup:\n  components: [app]\n")
        write("components/app/application.yaml", "value: default\n")
        write("components/app/application.base.yaml", "value: inherited\n")
    }

    @Test
    fun readPropertiesDirectlyFromFactory() {
        val destination = root.resolve("output")
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
            .withDestinationDir(destination.toFile())

        val properties = microconfig.dependencies().propertiesFactory()
            .getPropertiesOf("app", "app", "dev", listOf(APPLICATION))

        assertEquals(mapOf("value" to "inherited"), properties.propertiesAsKeyValue)
        assertEquals("dev", microconfig.inEnvironment("dev").name)
        MicroconfigRunner(microconfig).build("dev", emptyList(), emptyList())
        assertEquals("value: inherited\n", Files.readString(destination.resolve("app/application.yaml")))
    }

    @Test
    fun useComponentFactoryBeforeInitializingEnvironments() {
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
        val component = microconfig.dependencies().componentFactory().createComponent("alias", "app", "dev")

        val properties = component.getPropertiesFor { listOf(APPLICATION) }

        assertEquals(mapOf("value" to "inherited"), properties.propertiesAsKeyValue)
    }

    @Test
    fun useComponentGraphBeforeInitializingEnvironments() {
        val microconfig = Microconfig.searchConfigsIn(root.toFile())

        val files = microconfig.dependencies().componentGraph().getConfigFilesOf("app", "dev", APPLICATION)

        assertEquals(listOf("application.yaml", "application.base.yaml"), files.map { it.file.name })
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }
}
