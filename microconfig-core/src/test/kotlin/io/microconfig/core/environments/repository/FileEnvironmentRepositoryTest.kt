package io.microconfig.core.environments.repository

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.environments.Component
import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.ComponentGroup
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.impl.ComponentFactoryImpl
import io.microconfig.core.environments.repository.impl.FileEnvironmentRepository
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.util.stream.Collectors.toList

class FileEnvironmentRepositoryTest {
    val dir: File = classpathFile("envs/good")

    val configType: ConfigTypeRepository = mock(ConfigTypeRepository::class.java)

    val propertiesFactory: PropertiesFactory = mock(PropertiesFactory::class.java)

    val fsReader: FsReader = DumpedFsReader()

    val componentFactory: ComponentFactory = ComponentFactoryImpl(configType, propertiesFactory)

    val repo: FileEnvironmentRepository =
        FileEnvironmentRepository(dir, fsReader, componentFactory, propertiesFactory)

    @Test
    fun noEnvDirException() {
        assertThrows(
            IllegalArgumentException::class.java,
            {
                FileEnvironmentRepository(
                    classpathFile("configTypes"),
                    fsReader,
                    componentFactory,
                    propertiesFactory,
                )
            },
        )
    }

    @Test
    fun envExceptionWrappingOnFileRead() {
        val badReader: FsReader = mock(FsReader::class.java)
        `when`(badReader.readFully(any(File::class.java) ?: File("unused")))
            .thenThrow(IllegalArgumentException())
        val repo: FileEnvironmentRepository =
            FileEnvironmentRepository(dir, badReader, componentFactory, propertiesFactory)
        assertThrows(EnvironmentException::class.java, repo::environments)
    }

    @Test
    fun envExceptionWrappingOnGroupParse() {
        val badDir: File = classpathFile("envs/bad")
        val repo: FileEnvironmentRepository =
            FileEnvironmentRepository(badDir, fsReader, componentFactory, propertiesFactory)
        assertThrows(EnvironmentException::class.java, { repo.getByName("badGroup") })
    }

    @Test
    fun sameEnvNamesException() {
        val badDir: File = classpathFile("envs/bad")
        val repo: FileEnvironmentRepository =
            FileEnvironmentRepository(badDir, fsReader, componentFactory, propertiesFactory)
        assertThrows(EnvironmentException::class.java, { repo.getByName("bad") })
        assertThrows(EnvironmentException::class.java, { repo.getByName("bad2") })
        assertThrows(IllegalStateException::class.java, { repo.getByName("copy") })
    }

    @Test
    fun parseEnvFiles() {
        val environments: List<Environment> = repo.environments()
        assertEquals(5, environments.size)
        testDev(filter(environments, "dev"))
        testTest(filter(environments, "test"))
        testStaging(filter(environments, "staging"))
        testProd(filter(environments, "prod"))
        testAlias(filter(environments, "alias"))
    }

    @Test
    fun filterMethods() {
        assertEquals(
            setOf("alias", "dev", "test", "staging", "prod"),
            repo.environmentNames(),
        )
        assertEquals("dev", repo.getByName("dev").name)
        assertThrows(EnvironmentException::class.java, { repo.getByName("bad") })
        val dev: Environment = repo.getOrCreateByName("dev")
        assertEquals("dev", dev.name)
        assertEquals(11, dev.allComponents.asList().size)
        val fake: Environment = repo.getOrCreateByName("fake")
        assertEquals("fake", fake.name)
        assertEquals(0, fake.allComponents.asList().size)
    }

    private fun testDev(env: Environment) {
        assertFalse(env.isAbstract)
        testGroup(env, "orders", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
            "payment-provider-mock",
        )
        testGroup(env, "infra", "service-discovery", "api-gateway")
        testGroup(env, "kafka", "zookeeper", "kafka")
    }

    private fun testTest(env: Environment) {
        assertFalse(env.isAbstract)
        testGroup(env, "orders", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
            "payment-provider-mock",
        )
        testGroup(env, "infra", "service-discovery", "api-gateway", "selenium")
        testGroup(env, "kafka", "zookeeper", "kafka")
    }

    private fun testStaging(env: Environment) {
        assertFalse(env.isAbstract)
        testGroup(env, "orders", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
        )
        testGroup(env, "infra", "service-discovery", "api-gateway")
        testGroup(env, "kafka", "zookeeper", "kafka")
        testGroup(env, "monitoring", "grafana", "prometheus")
    }

    private fun testProd(env: Environment) {
        assertFalse(env.isAbstract)
        testGroup(env, "orders", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
        )
        testGroup(env, "infra", "service-discovery", "api-gateway")
        testGroup(env, "monitoring", "grafana", "prometheus")
    }

    private fun testAlias(env: Environment) {
        assertFalse(env.isAbstract)
        testGroup(env, "group", "componentAlias1", "componentAlias2")
    }

    private fun testGroup(
        environment: Environment,
        groupName: String,
        vararg components: String,
    ) {
        val group: ComponentGroup = environment.getGroupWithName(groupName)
        testComponents(environment.name, components.toList(), group.components.asList())
    }

    private fun testComponents(env: String, expected: List<String>, actual: List<Component>) {
        assertEquals(expected, actual.stream().map { it.name }.collect(toList()))
        actual.forEach({ c -> assertEquals(env, c.environment) })
    }

    private fun filter(envs: List<Environment>, name: String): Environment {
        return envs
            .stream()
            .filter({ e -> e.name.equals(name) })
            .findFirst()
            .orElseThrow(::IllegalStateException)
    }
}
