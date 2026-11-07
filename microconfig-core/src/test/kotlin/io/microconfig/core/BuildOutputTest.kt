package io.microconfig.core

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.configtypes.impl.ConfigTypeImpl
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.core.properties.serializers.PropertySerializers
import io.microconfig.utils.Os
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.function.BiConsumer

class BuildOutputTest {
    @Test
    fun refreshResolvedPrefixesForEachPreparationAfterAnAncestorSymlinkChanges() {
        assumeFalse(Os.isWindows())
        val first = directory.resolve("first/nested")
        val second = directory.resolve("second/nested")
        Files.createDirectories(first)
        Files.createDirectories(second)
        Files.createDirectories(destination)
        Files.createSymbolicLink(destination.resolve("one"), first.parent)
        Files.createSymbolicLink(destination.resolve("two"), second.parent)
        val units = listOf("one/nested" to "first", "two/nested" to "second").map { (name, value) ->
            val property = PropertyImpl.property("value", value, YAML, null)
            TypedPropertiesImpl(APPLICATION, name, "dev", mapOf("value" to property), emptyList())
        }
        val serializer = PropertySerializers.toFileIn(destination.toFile(), BiConsumer { _, _ -> })
        serializer.prepare(units)
        Files.delete(destination.resolve("two"))
        Files.createSymbolicLink(destination.resolve("two"), first.parent)

        assertThrows(IllegalArgumentException::class.java) { serializer.prepare(units) }
        assertFalse(Files.exists(first.resolve("application.yaml")))
        assertFalse(Files.exists(second.resolve("application.yaml")))
    }

    @TempDir
    lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/app/application.yaml", "value: current\n")
    }

    @Test
    fun rejectCrossTypeTemplateCollisionBeforeWritingConfigs() {
        previousConfig()
        write(
            "components/app/template.process",
            "mc.file.t: \${this@configDir}/source.txt -> application.yaml\n",
        )
        write("components/app/source.txt", "replacement")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
    }

    @Test
    fun rejectConflictingTemplatesFromDifferentTypes() {
        previousConfig()
        write(
            "components/app/application.yaml",
            "value: new\nmc.file.t: \${this@configDir}/one.txt -> shared.txt\n",
        )
        write(
            "components/app/template.process",
            "mc.file.t: \${this@configDir}/two.txt -> shared.txt\n",
        )
        write("components/app/one.txt", "one")
        write("components/app/two.txt", "two")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
        assertFalse(Files.exists(output("shared.txt")))
    }

    @Test
    fun rejectDuplicateTemplateDestinationsEvenWhenContentsMatch() {
        previousConfig()
        write(
            "components/app/application.yaml",
            "mc.file.t: \${this@configDir}/one.txt -> shared.txt\n",
        )
        write(
            "components/app/template.process",
            "mc.file.t: \${this@configDir}/two.txt -> shared.txt\n",
        )
        write("components/app/one.txt", "identical")
        write("components/app/two.txt", "identical")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
        assertFalse(Files.exists(output("shared.txt")))
    }

    @Test
    fun allowRepeatedConfigTypeDefinitionsWithIdenticalOutput() {
        write("microconfig.yaml", "configTypes: [app, app]\n")
        write("components/app/settings.app", "value: retained\n")
        build()
        assertEquals("value: retained\n", Files.readString(output("app.yaml")))
        write("components/app/settings.app", "value: changed\n")
        build()
        assertEquals("value: changed\n", Files.readString(output("app.yaml")))
        assertEquals("value: retained -> changed\n", Files.readString(output("diff-app.yaml")))
    }

    @Test
    fun preserveTemplateWrittenThroughDanglingSymlink() {
        assumeFalse(Os.isWindows())
        Files.createDirectories(output("alias.yaml").parent)
        Files.createSymbolicLink(output("alias.yaml"), Path.of("application.yaml"))
        write(
            "components/app/application.yaml",
            "mc.file.t: \${this@configDir}/source.txt -> alias.yaml\n",
        )
        write("components/app/source.txt", "retained\n")
        build()
        assertEquals("retained\n", Files.readString(output("application.yaml")))
        assertEquals("retained\n", Files.readString(output("alias.yaml")))
    }

    @Test
    fun rejectSharedConfigDestinationsBeforeWriting() {
        previousConfig()
        write(
            "microconfig.yaml",
            """
            configTypes:
              - first:
                  sourceExtensions: [.one]
                  resultFileName: application
              - second:
                  sourceExtensions: [.two]
                  resultFileName: application
            """.trimIndent(),
        )
        write("components/app/config.one", "value: same\n")
        write("components/app/config.two", "value: same\n")

        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
        assertFalse(Files.exists(output("diff-application.yaml")))
    }

    @Test
    fun rejectCollisionThroughDirectorySymlinkAndParentSegment() {
        assumeFalse(Os.isWindows())
        previousConfig()
        Files.createDirectory(output("nested"))
        Files.createSymbolicLink(output("alias"), Path.of("nested"))
        write(
            "components/app/application.yaml",
            "value: new\nmc.file.t: \${this@configDir}/source.txt -> alias/../application.yaml\n",
        )
        write("components/app/source.txt", "replacement")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun rejectTemplateAndGeneratedConfigSharingADestination(extension: String) {
        val separator = if (extension == "yaml") ": " else "="
        write(
            "components/app/application.yaml",
            "mc.file.t: \${this@configDir}/payload.txt -> process.$extension\n",
        )
        Files.createDirectories(output("process.$extension").parent)
        Files.writeString(output("process.$extension"), "value${separator}old\n")
        write("components/app/payload.txt", "value${separator}new" + if (extension == "yaml") "\n" else "")
        write("components/app/config.proc", "value${separator}new")

        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value${separator}old\n", Files.readString(output("process.$extension")))
        assertFalse(Files.exists(output("diff-process.$extension")))
    }

    @Test
    fun rejectTemplateAndAnotherComponentSharingADestination() {
        write("envs/dev.yaml", "group:\n  components: [app, worker]\n")
        write(
            "components/app/application.yaml",
            "mc.file.t: \${this@configDir}/payload.txt -> \${worker@resultDir}/application.yaml\n",
        )
        Files.createDirectories(destination.resolve("worker"))
        Files.writeString(destination.resolve("worker/application.yaml"), "value: old\n")
        write("components/app/payload.txt", "value: new\n")
        write("components/worker/application.yaml", "value: new\n")

        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(destination.resolve("worker/application.yaml")))
        assertFalse(Files.exists(destination.resolve("worker/diff-application.yaml")))
    }

    @Test
    fun removeUnownedObsoleteSymlinkAndKeepCurrentConfig() {
        assumeFalse(Os.isWindows())
        previousConfig()
        Files.createSymbolicLink(output("application.properties"), Path.of("application.yaml"))
        build()
        assertEquals("value: current\n", Files.readString(output("application.yaml")))
        assertFalse(Files.exists(output("application.properties"), NOFOLLOW_LINKS))
    }

    @Test
    fun keepPreviousDiffWhenTemplateCopyFails() {
        previousConfig()
        Files.writeString(output("diff-application.yaml"), "previous diff")
        Files.createDirectory(output("rendered.txt"))
        write(
            "components/app/application.yaml",
            "value: new\nmc.file.t: \${this@configDir}/source.txt -> rendered.txt\n",
        )
        write("components/app/source.txt", "payload")
        assertThrows(RuntimeException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
        assertEquals("previous diff", Files.readString(output("diff-application.yaml")))
    }

    @Test
    fun reportStaleDiffCleanupFailureAndRetainUnexpectedDirectory() {
        Files.createDirectories(output("diff-application.yaml"))
        Files.writeString(output("application.yaml"), "value: current\n")
        Files.writeString(output("diff-application.yaml/retained.txt"), "retained")
        assertThrows(IllegalStateException::class.java) { build() }
        assertEquals("retained", Files.readString(output("diff-application.yaml/retained.txt")))
    }

    @Test
    fun reuseSerializerForIndependentBuilds() {
        val serializer =
            PropertySerializers.toFileIn(destination.toFile(), PropertySerializers.withConfigDiff())
        write(
            "components/app/application.yaml",
            "mc.file.t: \${this@configDir}/source.txt -> application.yaml\n",
        )
        write("components/app/source.txt", "value: template\n")
        runner().buildProperties("dev", emptyList(), emptyList()).save(serializer)
        write("components/app/application.yaml", "value: config\n")
        runner().buildProperties("dev", emptyList(), emptyList()).save(serializer)
        assertEquals("value: config\n", Files.readString(output("application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun removePreviousDiffWhenATemplateTakesOverTheConfig(extension: String) {
        Files.deleteIfExists(root.resolve("components/app/application.yaml"))
        val source = "components/app/application.$extension"
        val separator = if (extension == "yaml") ": " else "="
        write(source, "value${separator}old\n")
        build()
        write(source, "value${separator}changed\n")
        build()
        assertTrue(Files.exists(output("diff-application.$extension")))
        write(source, "mc.template.config${separator}\${this@configDir}/template.txt -> application.$extension\n")
        write("components/app/template.txt", "value${separator}from-template\n")

        build()

        assertEquals("value${separator}from-template\n", Files.readString(output("application.$extension")))
        assertFalse(Files.exists(output("diff-application.$extension")))
        build()
        assertFalse(Files.exists(output("diff-application.$extension")))
    }

    @Test
    fun removeStaleDiffsWhenTemplatesOwnBothConfigFormats() {
        Files.createDirectories(output("application.yaml").parent)
        for (extension in listOf("yaml", "properties")) {
            Files.writeString(output("application.$extension"), "old")
            Files.writeString(output("diff-application.$extension"), "stale")
        }
        write(
            "components/app/application.yaml", """
            mc.file.yaml: ${'$'}{this@configDir}/yaml.txt -> application.yaml
            mc.file.properties: ${'$'}{this@configDir}/properties.txt -> application.properties
            """.trimIndent()
        )
        write("components/app/yaml.txt", "value: template\n")
        write("components/app/properties.txt", "value=template\n")

        build()

        assertEquals("value: template\n", Files.readString(output("application.yaml")))
        assertEquals("value=template\n", Files.readString(output("application.properties")))
        assertFalse(Files.exists(output("diff-application.yaml")))
        assertFalse(Files.exists(output("diff-application.properties")))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun rejectDuplicateConfigAndTemplateDestinationsInEitherOrder(reverse: Boolean) {
        val types = listOf("first", "second").let { if (reverse) it.reversed() else it }
        write("microconfig.yaml", "configTypes:\n" + types.joinToString("") { type ->
            "  - $type:\n      sourceExtensions: [.$type]\n      resultFileName: settings\n"
        })
        write("components/app/config.second", "mc.file.t: \${this@configDir}/template.txt -> settings.yaml\n")
        Files.createDirectories(output("settings.yaml").parent)
        Files.writeString(output("settings.yaml"), "value: old\n")
        Files.writeString(output("diff-settings.yaml"), "previous diff")
        write("components/app/config.first", "value: new\n")
        write("components/app/template.txt", "value: new\n")

        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("settings.yaml")))
        assertEquals("previous diff", Files.readString(output("diff-settings.yaml")))
    }

    @Test
    fun keepPreviousDiffUntilTheTemplateOwnerWritesSuccessfully() {
        write("components/app/application.yaml", "{}\n")
        write("components/app/config.proc", "mc.file.t: \${this@configDir}/source.txt -> application.yaml\n")
        write("components/app/source.txt", "value: template\n")
        previousConfig()
        Files.writeString(output("diff-application.yaml"), "previous diff")
        val units = runner().buildProperties("dev", emptyList(), emptyList()).asTypedProperties()
        val prepared = runner().toFiles().prepare(units)
        Files.delete(output("application.yaml"))
        Files.createDirectory(output("application.yaml"))

        units.first { it.configType.typeName == "app" }.save(prepared)

        assertEquals("previous diff", Files.readString(output("diff-application.yaml")))
        assertThrows(RuntimeException::class.java) {
            units.first { it.configType.typeName == "process" }.save(prepared)
        }
        assertEquals("previous diff", Files.readString(output("diff-application.yaml")))
    }

    @Test
    fun writePreparedOutputsInReverseOrderUsingTheOriginalDiffSnapshots() {
        write("envs/dev.yaml", "group:\n  components: [app, worker]\n")
        write("components/worker/application.yaml", "value: old\n")
        write("components/app/application.yaml", "value: old\n")
        build()
        write("components/app/application.yaml", "value: new\n")
        write("components/worker/application.yaml", "value: new\n")
        val units = runner().buildProperties("dev", emptyList(), emptyList()).asTypedProperties()
        val serializer = runner().toFiles().prepare(units)

        units.reversed().forEach { it.save(serializer) }

        listOf("app", "worker").forEach { component ->
            assertEquals("value: new\n", Files.readString(destination.resolve("$component/application.yaml")))
            assertEquals(
                "value: old -> new\n",
                Files.readString(destination.resolve("$component/diff-application.yaml"))
            )
        }
    }

    @Test
    fun keepPreparedRemovalDestinationWhenThePreviousFileDisappears() {
        write("components/app/application.yaml", "{}\n")
        Files.createDirectories(output("application.properties").parent)
        Files.writeString(output("application.properties"), "value=old")
        val units = runner().buildProperties("dev", emptyList(), emptyList()).asTypedProperties()
        val serializer = runner().toFiles().prepare(units)
        Files.delete(output("application.properties"))

        val files = units.map { it.save(serializer) }

        assertTrue(files.any { it.toPath() == output("application.properties") })
        assertEquals("-value=old", Files.readString(output("diff-application.properties")).trim())
        assertFalse(Files.exists(output("application.yaml")))
    }

    @Test
    fun preserveOppositeFormatsOwnedByDifferentConfigTypes() {
        write(
            "microconfig.yaml",
            "configTypes:\n  - one:\n      sourceExtensions: [.first]\n      resultFileName: settings\n  - two:\n      sourceExtensions: [.second]\n      resultFileName: settings\n",
        )
        write("components/app/config.first", "one: value\n")
        write("components/app/config.second", "two=value\n")
        build()
        assertEquals("one: value\n", Files.readString(output("settings.yaml")))
        assertEquals("two=value", Files.readString(output("settings.properties")))
    }

    @Test
    fun preserveDiffsForOppositeFormatsOwnedByDifferentConfigTypes() {
        write(
            "microconfig.yaml",
            "configTypes:\n  - one:\n      sourceExtensions: [.first]\n      resultFileName: settings\n  - two:\n      sourceExtensions: [.second]\n      resultFileName: settings\n",
        )
        write("components/app/config.first", "one: new\n")
        write("components/app/config.second", "two=new\n")
        Files.createDirectories(output("settings.yaml").parent)
        Files.writeString(output("settings.yaml"), "one: old\n")
        Files.writeString(output("settings.properties"), "two=old\n")
        build()
        assertEquals("one: old -> new\n", Files.readString(output("diff-settings.yaml")))
        assertEquals("two=old -> new", Files.readString(output("diff-settings.properties")))
    }

    @Test
    fun rejectConfigUsingReservedDiffNameBeforeWriting() {
        write(
            "microconfig.yaml",
            "configTypes:\n  - one:\n      sourceExtensions: [.first]\n      resultFileName: settings\n  - two:\n      sourceExtensions: [.second]\n      resultFileName: diff-settings\n",
        )
        write("components/app/config.first", "one: new\n")
        write("components/app/config.second", "two: new\n")
        Files.createDirectories(output("settings.yaml").parent)
        Files.writeString(output("settings.yaml"), "one: old\n")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("one: old\n", Files.readString(output("settings.yaml")))
        assertFalse(Files.exists(output("diff-settings.yaml")))
    }

    @Test
    fun rejectEmptyReservedConfigBeforeRemovingExistingDiff() {
        Files.createDirectories(output("diff-application.yaml").parent)
        Files.writeString(output("diff-application.yaml"), "retained")
        val serializer =
            PropertySerializers.toFileIn(destination.toFile(), PropertySerializers.withConfigDiff())
        assertThrows(IllegalArgumentException::class.java) {
            serializer.serialize(
                emptyList(),
                emptyList(),
                ConfigTypeImpl.byName("diff-application"),
                "app",
                "dev",
            )
        }
        assertEquals("retained", Files.readString(output("diff-application.yaml")))
    }

    @Test
    fun rejectCyclicOutputSymlinksBeforeWriting() {
        assumeFalse(Os.isWindows())
        Files.createDirectories(output("application.yaml").parent)
        Files.createSymbolicLink(output("application.yaml"), Path.of("alias.yaml"))
        Files.createSymbolicLink(output("alias.yaml"), Path.of("application.yaml"))
        val error = assertThrows(IllegalArgumentException::class.java) { build() }
        assertTrue(error.message.orEmpty().contains("Cyclic output symlink"), error.message)
        assertTrue(Files.isSymbolicLink(output("application.yaml")))
        assertTrue(Files.isSymbolicLink(output("alias.yaml")))
    }

    @Test
    fun preserveLegacyListenerBeforeWriteSemantics() {
        previousConfig()
        var previous: String? = null
        val listener =
            BiConsumer<java.io.File, Collection<io.microconfig.core.properties.Property>> { file, _
                ->
                if (file.name == "application.yaml") previous = file.readText()
            }
        runner()
            .buildProperties("dev", emptyList(), emptyList())
            .save(PropertySerializers.toFileIn(destination.toFile(), listener))
        assertEquals("value: old\n", previous)
        assertEquals("value: current\n", Files.readString(output("application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["diff-application.yaml", "diff-application.properties"])
    fun rejectReservedDiffTemplateNameWhenConfigIsEmpty(name: String) {
        write(
            "components/app/application.yaml",
            "mc.file.t: \${this@configDir}/source.txt -> $name\n",
        )
        write("components/app/source.txt", "payload\n")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertFalse(Files.exists(output(name)))
    }

    @Test
    fun rejectTemplateUsingReservedDiffName() {
        previousConfig()
        write(
            "components/app/application.yaml",
            "value: new\nmc.file.t: \${this@configDir}/source.txt -> diff-application.yaml\n",
        )
        write("components/app/source.txt", "payload\n")
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals("value: old\n", Files.readString(output("application.yaml")))
        assertFalse(Files.exists(output("diff-application.yaml")))
    }

    @Test
    fun rejectReservedDiffTemplateNameWithPlainListener() {
        write(
            "components/app/application.yaml",
            "value: new\nmc.file.t: \${this@configDir}/source.txt -> diff-application.yaml\n",
        )
        write("components/app/source.txt", "payload\n")
        val serializer = PropertySerializers.toFileIn(destination.toFile(), BiConsumer { _, _ -> })
        assertThrows(IllegalArgumentException::class.java) {
            runner().buildProperties("dev", emptyList(), emptyList()).save(serializer)
        }
        assertFalse(Files.exists(output("diff-application.yaml")))
    }

    private fun previousConfig() {
        Files.createDirectories(output("application.yaml").parent)
        Files.writeString(output("application.yaml"), "value: old\n")
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build() = runner().build("dev", emptyList(), emptyList())

    private fun runner() = MicroconfigRunner(root.toFile(), destination.toFile())

    private fun output(name: String) = destination.resolve("app/$name")
}
