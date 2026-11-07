package io.microconfig.core.properties.io.yaml

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.impl.PropertiesImpl
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo
import io.microconfig.core.properties.serializers.PropertySerializers
import io.microconfig.io.impl.DumpedFsReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.function.BiConsumer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class YamlWriterConcurrencyTest {
    @TempDir lateinit var directory: Path

    @Test
    fun serializeDistinctPreparedOutputsConcurrentlyAndNotifyEachOnce() {
        val base = newConfigIo(DumpedFsReader())
        val entered = CountDownLatch(2)
        val notified = ConcurrentHashMap<File, Int>()
        val io = object : ConfigIo by base {
            override fun writeTo(file: File): ConfigWriter {
                val writer = base.writeTo(file)
                return object : ConfigWriter by writer {
                    override fun write(properties: Collection<Property>) {
                        entered.countDown()
                        assertTrue(entered.await(5, SECONDS), "Distinct files must write concurrently")
                        writer.write(properties)
                    }
                }
            }
        }
        val units = (0..31).map { index ->
            val property = PropertyImpl.property("value", "complete-$index", YAML, null)
            TypedPropertiesImpl(APPLICATION, "app$index", "dev", mapOf("value" to property), emptyList())
        }
        val prepared = PropertySerializers.toFileIn(directory.toFile(),
            BiConsumer { file, _ -> notified.merge(file, 1, Int::plus) }, io).prepare(units)
        val pool = ForkJoinPool(4)
        try {
            pool.submit { units.parallelStream().forEach { it.save(prepared) } }.get(20, SECONDS)
        } finally {
            pool.shutdownNow()
        }
        assertEquals(32, notified.size)
        units.forEachIndexed { index, _ ->
            val file = directory.resolve("app$index/application.yaml").toFile()
            assertEquals(1, notified[file])
            assertEquals(mapOf("value" to "complete-$index"), Yaml().load<Map<*, *>>(file.readText()))
        }
    }

    @Test
    fun refreshSerializedContentWhenValuesFormatsAndVariableFlagsChange() {
        val file = directory.resolve("application.yaml").toFile()
        val writer = newConfigIo(DumpedFsReader()).writeTo(file)
        var value = "true"
        var literal = false
        var variable = false
        var keyFormat = YAML
        val base = PropertyImpl.property("true", value, YAML, null)
        val property = object : Property by base {
            override val value get() = value
            override val isLiteralText get() = literal
            override val isVar get() = variable
            override val keyFormat get() = keyFormat
        }
        val properties = listOf(property)
        assertEquals(mapOf(true to true), Yaml().load<Map<Any?, Any?>>(writer.serialize(properties)))
        literal = true
        assertEquals(mapOf(true to "true"), Yaml().load<Map<Any?, Any?>>(writer.serialize(properties)))
        keyFormat = ConfigFormat.PROPERTIES
        assertEquals(mapOf("true" to "true"), Yaml().load<Map<Any?, Any?>>(writer.serialize(properties)))
        value = "changed"
        writer.write(properties)
        assertEquals(mapOf("true" to "changed"), Yaml().load<Map<Any?, Any?>>(file.readText()))
        variable = true
        assertEquals("", writer.serialize(properties))
    }

    @Test
    fun serializeIndependentValuesConcurrentlyWithoutSharingYamlState() {
        val file = directory.resolve("application.yaml").toFile()
        val writer = newConfigIo(DumpedFsReader()).writeTo(file)
        val pool = ForkJoinPool(4)
        try {
            repeat(4) {
                val rendered = pool.submit<List<Pair<Int, String>>> {
                    (0..127).toList().parallelStream().map { index ->
                        index to writer.serialize(listOf(
                            PropertyImpl.property("label", "item-$index", YAML, null),
                            PropertyImpl.property("number", index.toString(), YAML, null)))
                    }.toList()
                }.get(20, SECONDS)
                rendered.forEach { (index, text) ->
                    assertEquals(mapOf("label" to "item-$index", "number" to index), Yaml().load<Map<*, *>>(text))
                }
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun prepareYamlContentWithoutWritingFiles() {
        val io = newConfigIo(DumpedFsReader())
        val file = directory.resolve("app/application.yaml").toFile()
        val properties = listOf(PropertyImpl.property("value", "ready", YAML, null))

        val unit = TypedPropertiesImpl(APPLICATION, "app", "dev", properties.associateBy { it.key }, emptyList())
        val prepared = PropertySerializers.toFileIn(directory.toFile(), BiConsumer { _, _ -> }, io)
            .prepare(listOf(unit))

        assertFalse(file.exists())
        unit.save(prepared)
        assertEquals(mapOf("value" to "ready"), Yaml().load<Map<*, *>>(file.readText()))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun keepWriteOnlyCustomWritersOnTheCallingThreadIncludingDelegatingWrappers(delegated: Boolean) {
        val base = newConfigIo(DumpedFsReader())
        val caller = Thread.currentThread()
        val written = mutableListOf<Collection<Property>>()
        fun writer(file: File): ConfigWriter = object : ConfigWriter by base.writeTo(file) {
            override fun serialize(properties: Collection<Property>): String =
                throw UnsupportedOperationException("Write-only custom writer")

            override fun write(properties: Collection<Property>) {
                assertSame(caller, Thread.currentThread())
                written.add(properties)
            }
        }
        val io = if (delegated) object : ConfigIo by base {
            override fun writeTo(file: File): ConfigWriter = writer(file)
        } else object : ConfigIo {
            override fun readFrom(file: File): ConfigReader = base.readFrom(file)
            override fun writeTo(file: File): ConfigWriter = writer(file)
        }
        val property = PropertyImpl.property("value", "kept", YAML, null)
        val unit = TypedPropertiesImpl(APPLICATION, "app", "dev", mapOf("value" to property), emptyList())

        PropertiesImpl(listOf(unit)).save(PropertySerializers.toFileIn(directory.toFile(), BiConsumer { _, _ -> }, io))

        assertEquals(listOf(listOf(property)), written)
        assertFalse(Files.exists(directory.resolve("app/application.yaml")))
    }
}
