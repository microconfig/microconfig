package io.microconfig.core

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo
import io.microconfig.core.properties.serializers.PropertySerializers
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Predicate

class CustomFsReaderSerializationTest {
    @TempDir
    lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createEnvironment() {
        write(root.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun compareAgainstPreviousContentFromCustomReader(extension: String) {
        val runner = runner(extension, "virtual-old", "virtual-new")

        runner.build("dev", emptyList(), emptyList())

        assertEquals(
            content(extension, "virtual-new").trim(),
            Files.readString(output(extension)).trim(),
        )
        assertEquals(
            content(extension, "virtual-old -> virtual-new").trim(),
            Files.readString(diff(extension)).trim(),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun removeStaleDiffWhenCustomReaderValuesHaveNotChanged(extension: String) {
        val runner = runner(extension, "virtual-value", "virtual-value")
        write(diff(extension), "stale diff")

        runner.build("dev", emptyList(), emptyList())

        assertEquals(
            content(extension, "virtual-value").trim(),
            Files.readString(output(extension)).trim(),
        )
        assertFalse(Files.exists(diff(extension)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun keepReadersIsolatedBetweenMicroconfigInstances(extension: String) {
        val first = runner(extension, "first-old", "virtual-new")
        val second = runner(extension, "second-old", "virtual-new")

        first.build("dev", emptyList(), emptyList())
        assertEquals(
            content(extension, "first-old -> virtual-new").trim(),
            Files.readString(diff(extension)).trim(),
        )
        second.build("dev", emptyList(), emptyList())
        assertEquals(
            content(extension, "second-old -> virtual-new").trim(),
            Files.readString(diff(extension)).trim(),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun useSuppliedConfigIoForStringAndFileSerialization(extension: String) {
        val written = mutableMapOf<File, String>()
        val base = newConfigIo(DumpedFsReader())
        val configIo =
            object : ConfigIo by base {
                override fun writeTo(file: File): ConfigWriter {
                    val writer = base.writeTo(file)
                    return object : ConfigWriter by writer {
                        override fun write(properties: Collection<Property>) {
                            written[file.canonicalFile] = serialize(properties)
                        }

                        override fun serialize(properties: Collection<Property>): String =
                            "# custom\n" + writer.serialize(properties)
                    }
                }
            }
        val properties =
            runner(extension, "virtual-old", "virtual-new")
                .buildProperties("dev", emptyList(), emptyList())

        val text = properties.first().save(PropertySerializers.asString(configIo))
        properties.save(
            PropertySerializers.toFileIn(destination.toFile(), BiConsumer { _, _ -> }, configIo)
        )

        assertEquals("# custom\n" + content(extension, "virtual-new").trim(), text.trim())
        assertEquals(mapOf(output(extension).toFile().canonicalFile to text), written)
    }

    @Test
    fun keepDefaultConfigIoFormatCachesIndependent() {
        val file = root.resolve("settings.custom")
        write(file, "value: yaml\n")
        assertEquals(mapOf("value" to "yaml"), configIo().readFrom(file.toFile()).propertiesAsMap())

        write(file, "value=properties\n")

        assertEquals(
            mapOf("value" to "properties"),
            configIo().readFrom(file.toFile()).propertiesAsMap(),
        )
    }

    private fun runner(extension: String, previous: String, current: String): MicroconfigRunner {
        val source = root.resolve("components/app/application.$extension")
        val target = output(extension)
        write(source, content(extension, "disk-source"))
        write(target, content(extension, "disk-old"))
        val reader =
            readerFor(
                mapOf(source to content(extension, current), target to content(extension, previous))
            )
        val microconfig =
            Microconfig.searchConfigsIn(root.toFile())
                .withDestinationDir(destination.toFile())
                .withFsReader(reader)
        return MicroconfigRunner(microconfig)
    }

    private fun readerFor(content: Map<Path, String>): FsReader {
        val files = content.mapKeys { it.key.toFile().canonicalFile }
        val fallback = DumpedFsReader()
        return object : FsReader {
            override fun readFully(file: File): String =
                files[file.canonicalFile] ?: fallback.readFully(file)

            override fun readLines(file: File): List<String> = readFully(file).reader().readLines()

            override fun firstLineOf(file: File, predicate: Predicate<String>): Optional<String> =
                Optional.ofNullable(readLines(file).firstOrNull { predicate.test(it) })
        }
    }

    private fun content(extension: String, value: String): String =
        if (extension == "yaml") "value: $value\n" else "value=$value\n"

    private fun output(extension: String) = destination.resolve("app/application.$extension")

    private fun diff(extension: String) = destination.resolve("app/diff-application.$extension")

    private fun write(path: Path, content: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }
}
