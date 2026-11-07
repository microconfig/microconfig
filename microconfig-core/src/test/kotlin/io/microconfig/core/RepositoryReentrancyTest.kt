package io.microconfig.core

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.PropertyImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit.SECONDS

class RepositoryReentrancyTest {
    @TempDir
    lateinit var root: Path

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun resolveRepositoryDependenciesThroughThePublicResolver(parallel: Boolean) {
        createRepository(parallel)
        val entered = CountDownLatch(if (parallel) 2 else 0)
        lateinit var configured: Microconfig
        val repository = PropertiesRepository { component, environment, type ->
            if (type.typeName != "app" || component !in setOf("Aa", "C", "BB")) emptyMap()
            else {
                val source = DeclaringComponentImpl(type.typeName, component, environment)
                val value = if (component == "BB") "leaf" else {
                    entered.countDown()
                    check(entered.await(5, SECONDS)) { "Expected concurrent repository requests" }
                    val dependency = if (component == "Aa") "C" else "BB"
                    configured.resolver().resolve("\${$dependency@value}", source, source)
                }
                mapOf("value" to PropertyImpl.property("value", value, YAML, source))
            }
        }
        configured = Microconfig.searchConfigsIn(root.toFile())
            .withAdditionalPropertiesRepositories(listOf(repository))
        val pool = ForkJoinPool(2)
        try {
            val result = pool.submit<Properties> {
                MicroconfigRunner(configured).buildProperties("dev", emptyList(), emptyList())
            }.get(15, SECONDS)
            val values = result.asTypedProperties().filter { it.configType.typeName == "app" }
                .associate { it.declaringComponent.component to it.propertiesAsKeyValue.getValue("value") }

            val expected = if (parallel) mapOf("first" to "leaf", "second" to "leaf") else mapOf("first" to "leaf")
            assertEquals(expected, values)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun createRepository(parallel: Boolean) {
        val components = if (parallel) "first, second" else "first"
        write("envs/dev.yaml", "group:\n  components: [$components]\n")
        write("components/first/application.yaml", "value: \${Aa@value}\n")
        if (parallel) write("components/second/application.yaml", "value: \${C@value}\n")
        listOf("Aa", "C", "BB").forEach { write("components/$it/application.yaml", "# Provided by the repository\n") }
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }
}
