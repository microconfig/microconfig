package io.microconfig.core.properties.resolvers.placeholder.strategies

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.configtypes.StandardConfigType.PROCESS
import io.microconfig.core.environments.Component
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.impl.PropertiesFactoryImpl
import io.microconfig.core.properties.resolvers.placeholder.strategies.standard.impl.StandardResolveStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class StandardResolveStrategyTest {
    @Test
    fun reuseUnresolvedPropertiesAndSeparateAliasesEnvironmentsAndTypes() {
        val reads = mutableListOf<String>()
        val factory = PropertiesFactoryImpl { _, environment, type ->
            mapOf("value" to PropertyImpl.property("value", "$environment/${type.typeName}", YAML, null))
        }
        val repository = mock(EnvironmentRepository::class.java)
        for (environment in listOf("dev", "prod")) {
            val env = mock(Environment::class.java)
            `when`(repository.getOrCreateByName(environment)).thenReturn(env)
            for (name in listOf("one", "two")) {
                val component = object : Component {
                    override val name = name
                    override val originalName = "original"
                    override val environment = environment

                    override fun getPropertiesFor(filter: ConfigTypeFilter): Properties {
                        val types = filter.selectTypes(listOf(APPLICATION, PROCESS))
                        reads.add("$name/$environment/${types.single().typeName}")
                        return factory.getPropertiesOf(name, originalName, environment, types)
                    }
                }
                `when`(env.findComponentWithName(name)).thenReturn(component)
            }
        }
        val strategy = StandardResolveStrategy(repository)

        repeat(2) {
            assertEquals("dev/app", strategy.resolve("one", "value", "dev", "app", "root").orElseThrow().value)
            assertEquals("prod/app", strategy.resolve("one", "value", "prod", "app", "root").orElseThrow().value)
            assertEquals("dev/process", strategy.resolve("one", "value", "dev", "process", "root").orElseThrow().value)
            assertEquals("dev/app", strategy.resolve("two", "value", "dev", "app", "root").orElseThrow().value)
            assertEquals(true, strategy.resolve("one", "missing", "dev", "app", "root").isEmpty)
        }

        assertEquals(listOf("one/dev/app", "one/prod/app", "one/dev/process", "two/dev/app"), reads)
    }
}
