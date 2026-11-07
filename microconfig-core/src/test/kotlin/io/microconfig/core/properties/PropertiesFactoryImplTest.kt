package io.microconfig.core.properties

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.configtypes.StandardConfigType.PROCESS
import io.microconfig.core.properties.impl.PropertiesFactoryImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

class PropertiesFactoryImplTest {
    @Test
    fun shareRawReadsBetweenAliasesAndKeepEnvironmentAndTypeSeparate() {
        val reads = mutableListOf<String>()
        val factory = PropertiesFactoryImpl { component, environment, type ->
            reads.add("$component/$environment/${type.typeName}")
            mapOf("value" to PropertyImpl.property("value", environment, ConfigFormat.YAML, null))
        }

        val first = factory.getPropertiesOf("one", "original", "dev", listOf(APPLICATION))
        val second = factory.getPropertiesOf("two", "original", "dev", listOf(APPLICATION))
        factory.getPropertiesOf("one", "original", "prod", listOf(APPLICATION))
        factory.getPropertiesOf("one", "original", "dev", listOf(PROCESS))

        assertEquals("one", first.first().declaringComponent.component)
        assertEquals("two", second.first().declaringComponent.component)
        assertEquals(listOf("original/dev/app", "original/prod/app", "original/dev/process"), reads)
    }

    @Test
    fun resolveNestedRepositoryRequestsWithCollidingKeys() {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val reads = mutableListOf<String>()
        lateinit var factory: PropertiesFactoryImpl
        factory = PropertiesFactoryImpl { component, environment, type ->
            reads.add(component)
            if (component == "Aa")
                factory.getPropertiesOf("BB", "BB", environment, listOf(type)).first().propertiesAsMap
            else rawValue("base")
        }

        val first = factory.getPropertiesOf("first", "Aa", "dev", listOf(APPLICATION)).first()
        val second = factory.getPropertiesOf("second", "BB", "dev", listOf(APPLICATION)).first()

        assertEquals(mapOf("value" to "base"), first.propertiesAsKeyValue)
        assertSame(first.properties.single(), second.properties.single())
        assertEquals(listOf("Aa", "BB"), reads)
    }

    @Test
    fun completeConcurrentAcyclicDependenciesWhenTheLeafCollidesWithTheRoot() {
        val entered = CountDownLatch(2)
        lateinit var factory: PropertiesFactoryImpl
        factory = PropertiesFactoryImpl { component, environment, type ->
            if (component == "BB") rawValue("leaf")
            else {
                entered.countDown()
                check(entered.await(5, SECONDS)) { "Both repository requests must start concurrently" }
                val dependency = if (component == "Aa") "C" else "BB"
                factory.getPropertiesOf(dependency, dependency, environment, listOf(type)).first().propertiesAsMap
            }
        }
        val pool = daemonPool()
        try {
            val first = pool.submit<Properties> { factory.getPropertiesOf("Aa", "Aa", "dev", listOf(APPLICATION)) }
            val second = pool.submit<Properties> { factory.getPropertiesOf("C", "C", "dev", listOf(APPLICATION)) }

            assertEquals(mapOf("value" to "leaf"), first.get(10, SECONDS).first().propertiesAsKeyValue)
            assertEquals(mapOf("value" to "leaf"), second.get(10, SECONDS).first().propertiesAsKeyValue)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun publishOneSnapshotForAliasesAfterConcurrentRepositoryReads() {
        val entered = CountDownLatch(2)
        val reads = AtomicInteger()
        val factory = PropertiesFactoryImpl { _, _, _ ->
            val value = reads.incrementAndGet()
            entered.countDown()
            check(entered.await(5, SECONDS)) { "Repository reads must run outside cache locks" }
            rawValue("read-$value")
        }
        val pool = daemonPool()
        try {
            val tasks = listOf("first", "second").map { alias ->
                pool.submit<TypedProperties> {
                    factory.getPropertiesOf(alias, "original", "dev", listOf(APPLICATION)).first()
                }
            }
            val (first, second) = tasks.map { it.get(10, SECONDS) }
            val third = factory.getPropertiesOf("third", "original", "dev", listOf(APPLICATION)).first()

            assertSame(first.properties.single(), second.properties.single())
            assertSame(first.properties.single(), third.properties.single())
            assertEquals(listOf("first", "second", "third"),
                listOf(first, second, third).map { it.declaringComponent.component })
            assertEquals(2, reads.get())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun retryFailedRepositoryReadsWithoutCachingFailures() {
        val reads = AtomicInteger()
        val failure = IllegalArgumentException("Can't read repository")
        val factory = PropertiesFactoryImpl { _, _, _ ->
            if (reads.incrementAndGet() == 1) throw failure
            rawValue("recovered")
        }

        assertSame(failure, assertThrows(IllegalArgumentException::class.java) {
            factory.getPropertiesOf("first", "original", "dev", listOf(APPLICATION))
        })
        val recovered = factory.getPropertiesOf("second", "original", "dev", listOf(APPLICATION)).first()
        val cached = factory.getPropertiesOf("third", "original", "dev", listOf(APPLICATION)).first()

        assertEquals(mapOf("value" to "recovered"), recovered.propertiesAsKeyValue)
        assertSame(recovered.properties.single(), cached.properties.single())
        assertEquals(2, reads.get())
    }

    @Test
    fun rejectRepositoryCyclesAndAllowRetryAfterFailure() {
        var cyclic = true
        lateinit var factory: PropertiesFactoryImpl
        factory = PropertiesFactoryImpl { component, environment, type ->
            if (!cyclic) rawValue("recovered")
            else {
                val dependency = if (component == "Aa") "BB" else "Aa"
                factory.getPropertiesOf(dependency, dependency, environment, listOf(type)).first().propertiesAsMap
            }
        }

        val failure = assertThrows(IllegalStateException::class.java) {
            factory.getPropertiesOf("first", "Aa", "dev", listOf(APPLICATION))
        }

        assertTrue(failure.message.orEmpty().contains("Aa[dev]/app -> BB[dev]/app -> Aa[dev]/app"), failure.message)
        cyclic = false
        assertEquals(mapOf("value" to "recovered"),
            factory.getPropertiesOf("second", "Aa", "dev", listOf(APPLICATION)).first().propertiesAsKeyValue)
    }

    @Test
    fun resolveTheSameComponentThroughAnotherEnvironmentAndConfigType() {
        lateinit var factory: PropertiesFactoryImpl
        factory = PropertiesFactoryImpl { component, environment, type ->
            if (environment == "dev" && type == APPLICATION)
                factory.getPropertiesOf(component, component, "prod", listOf(PROCESS)).first().propertiesAsMap
            else rawValue("$component/$environment/${type.typeName}")
        }

        val result = factory.getPropertiesOf("alias", "original", "dev", listOf(APPLICATION)).first()

        assertEquals(mapOf("value" to "original/prod/process"), result.propertiesAsKeyValue)
        assertEquals("alias", result.declaringComponent.component)
    }

    private fun rawValue(value: String): Map<String, Property> =
        mapOf("value" to PropertyImpl.property("value", value, ConfigFormat.YAML, null))

    private fun daemonPool(): ExecutorService =
        Executors.newFixedThreadPool(2) { action ->
            Thread(action, "properties-factory-regression").apply { isDaemon = true }
        }
}
