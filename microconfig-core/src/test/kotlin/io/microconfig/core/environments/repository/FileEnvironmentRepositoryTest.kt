package io.microconfig.core.environments.repository

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.environments.*
import io.microconfig.core.environments.impl.ComponentFactoryImpl
import io.microconfig.core.environments.repository.impl.FileEnvironmentRepository
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import java.io.File
import java.util.Optional.ofNullable
import java.util.stream.Collectors.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

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
        assertEquals(0, env.portOffset)
        testGroup(env, "orders", "10.0.0.1", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "10.0.0.1",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
            "payment-provider-mock",
        )
        testGroup(env, "infra", "10.0.10.1", "service-discovery", "api-gateway")
        testGroup(env, "kafka", "10.0.0.1", "zookeeper", "kafka")
    }

    private fun testTest(env: Environment) {
        assertFalse(env.isAbstract)
        assertEquals(100, env.portOffset)
        testGroup(env, "orders", "10.0.0.1", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "10.0.0.1",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
            "payment-provider-mock",
        )
        testGroup(env, "infra", "10.0.10.1", "service-discovery", "api-gateway", "selenium")
        testGroup(env, "kafka", "10.0.0.1", "zookeeper", "kafka")
    }

    private fun testStaging(env: Environment) {
        assertFalse(env.isAbstract)
        assertEquals(0, env.portOffset)
        testGroup(env, "orders", "10.20.0.1", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "10.20.0.1",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
        )
        testGroup(env, "infra", "10.20.10.1", "service-discovery", "api-gateway")
        testGroup(env, "kafka", "10.20.0.1", "zookeeper", "kafka")
        testGroup(env, "monitoring", "10.20.20.1", "grafana", "prometheus")
    }

    private fun testProd(env: Environment) {
        assertFalse(env.isAbstract)
        assertEquals(100, env.portOffset)
        testGroup(env, "orders", "10.20.0.1", "order-db-patcher", "order-service", "order-ui")
        testGroup(
            env,
            "payments",
            "10.20.0.1",
            "payment-db-patcher",
            "payment-service",
            "payment-ui",
        )
        testGroup(env, "infra", "10.20.10.1", "service-discovery", "api-gateway")
        testGroup(env, "monitoring", "10.20.20.1", "grafana", "prometheus")
    }

    private fun testAlias(env: Environment) {
        assertFalse(env.isAbstract)
        assertEquals(0, env.portOffset)
        testGroup(env, "group", null, "componentAlias1", "componentAlias2")
    }

    private fun testGroup(
        environment: Environment,
        groupName: String,
        ip: String?,
        vararg components: String,
    ) {
        val group: ComponentGroup = environment.getGroupWithName(groupName)
        assertEquals(ofNullable(ip), group.ip)
        testComponents(environment.name, components.toList(), group.components.asList())
    }

    private fun testComponents(env: String, expected: List<String>, actual: List<Component>) {
        assertEquals(expected, actual.stream().map(Component::name).collect(toList()))
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
