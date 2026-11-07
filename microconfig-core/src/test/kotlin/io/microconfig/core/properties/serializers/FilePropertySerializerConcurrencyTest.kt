package io.microconfig.core.properties.serializers

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.impl.PropertiesImpl
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo
import io.microconfig.core.properties.serializers.impl.ConfigDiff
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer

class FilePropertySerializerConcurrencyTest {
    @TempDir
    lateinit var directory: Path

    @ParameterizedTest
    @CsvSource("YAML,true", "YAML,false", "PROPERTIES,true", "PROPERTIES,false")
    fun rejectDuplicateDestinationsBeforeAnyWrites(format: ConfigFormat, identical: Boolean) {
        val first = unit("first", format)
        val second = unit(if (identical) "first" else "second", format)
        val writes = AtomicInteger()
        val io = beforeWrite { _, _ -> writes.incrementAndGet() }
        val target = directory.resolve("app/application${format.extension()}").toFile()
        newConfigIo(DumpedFsReader()).writeTo(target).write(unit("initial", format).properties)
        val serializer = PropertySerializers.toFileIn(directory.toFile(), ConfigDiff(io), io)

        val failure = assertThrows(IllegalArgumentException::class.java) {
            PropertiesImpl(listOf(first, second)).save(serializer)
        }
        assertTrue(failure.message.orEmpty().contains("same destination"))
        assertEquals(0, writes.get())
        assertEquals("initial", readValue(target, format))
        assertFalse(File(target.parentFile, "diff-${target.name}").exists())
    }

    @Test
    fun allowAnotherSaveAfterAWriteFailure() {
        val failure = IllegalStateException("Write failed")
        val io = beforeWrite { _, properties ->
            if (properties.single().value == "broken") throw failure
        }
        val serializer = PropertySerializers.toFileIn(directory.toFile(), BiConsumer { _, _ -> }, io)
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            PropertiesImpl(listOf(unit("broken", YAML))).save(serializer)
        })

        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { PropertiesImpl(listOf(unit("recovered", YAML))).save(serializer) }.get(10, SECONDS)
            assertEquals("recovered", readValue(directory.resolve("app/application.yaml").toFile(), YAML))
        } finally {
            executor.shutdownNow()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun saveDifferentFilesConcurrently(sharedSerializer: Boolean) {
        val firstEntered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val io = beforeWrite { _, properties ->
            if (properties.single().value == "first") {
                firstEntered.countDown()
                check(release.await(10, SECONDS))
            }
        }
        val firstDirectory = directory.resolve("first").toFile()
        val secondDirectory = directory.resolve("second").toFile()
        val first = PropertySerializers.toFileIn(firstDirectory, BiConsumer { _, _ -> }, io)
        val second = if (sharedSerializer) first
        else PropertySerializers.toFileIn(secondDirectory, BiConsumer { _, _ -> }, io)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstSave = executor.submit { PropertiesImpl(listOf(unit("first", YAML))).save(first) }
            assertTrue(firstEntered.await(10, SECONDS))
            executor.submit { PropertiesImpl(listOf(unit("second", YAML, "other"))).save(second) }.get(5, SECONDS)
            val secondRoot = if (sharedSerializer) firstDirectory else secondDirectory
            assertEquals("second", readValue(File(secondRoot, "other/application.yaml"), YAML))
            release.countDown()
            firstSave.get(10, SECONDS)
            assertEquals("first", readValue(File(firstDirectory, "app/application.yaml"), YAML))
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun letAListenerWaitForAnotherFileSavedByTheSameSerializer() {
        val executor = Executors.newSingleThreadExecutor()
        lateinit var serializer: PropertySerializer<File>
        serializer = PropertySerializers.toFileIn(directory.toFile(), BiConsumer { file, _ ->
            if (file.parentFile.name == "app")
                executor.submit { unit("audit", YAML, "audit").save(serializer) }.get(5, SECONDS)
        })
        try {
            PropertiesImpl(listOf(unit("parent", YAML))).save(serializer)
            assertEquals("parent", readValue(directory.resolve("app/application.yaml").toFile(), YAML))
            assertEquals("audit", readValue(directory.resolve("audit/application.yaml").toFile(), YAML))
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, SECONDS))
        }
    }

    @Test
    fun letListenersPrepareEachOthersSerializersWithoutLockInversion() {
        val serializers = arrayOfNulls<PropertySerializer<File>>(2)
        val ready = CyclicBarrier(2)
        for (index in serializers.indices) {
            serializers[index] = PropertySerializers.toFileIn(
                directory.resolve("output$index").toFile(),
                BiConsumer { _, _ ->
                    ready.await(5, SECONDS)
                    serializers[1 - index]!!.prepare(emptyList())
                })
        }
        // A reintroduced monitor deadlock must fail the test without keeping the test JVM alive.
        val executor = Executors.newFixedThreadPool(2) { task -> Thread(task).apply { isDaemon = true } }
        try {
            serializers.map { serializer ->
                executor.submit { PropertiesImpl(listOf(unit("parent", YAML))).save(serializer!!) }
            }.forEach { it.get(10, SECONDS) }
            for (index in serializers.indices)
                assertEquals("parent", readValue(directory.resolve("output$index/app/application.yaml").toFile(), YAML))
        } finally {
            executor.shutdownNow()
        }
    }

    private fun unit(value: String, format: ConfigFormat, component: String = "app"): TypedPropertiesImpl {
        val property = PropertyImpl.property("value", value, format, null)
        return TypedPropertiesImpl(APPLICATION, component, "dev", mapOf("value" to property), emptyList())
    }

    private fun beforeWrite(action: (File, Collection<Property>) -> Unit): ConfigIo {
        val base = newConfigIo(DumpedFsReader())
        return object : ConfigIo by base {
            override fun writeTo(file: File): ConfigWriter {
                val writer = base.writeTo(file)
                return object : ConfigWriter by writer {
                    override fun write(properties: Collection<Property>) {
                        action(file, properties)
                        writer.write(properties)
                    }
                }
            }
        }
    }

    private fun readValue(file: File, format: ConfigFormat): String =
        if (format == YAML) Yaml().load<Map<String, String>>(file.readText()).getValue("value")
        else Properties().apply { load(StringReader(file.readText())) }.getProperty("value")
}
