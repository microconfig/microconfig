package io.microconfig.core

import io.microconfig.core.configtypes.impl.ConfigTypeImpl
import io.microconfig.core.properties.serializers.PropertySerializers
import io.microconfig.utils.Os
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.function.BiConsumer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BuildOutputTest {
    @TempDir lateinit var directory: Path
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
    fun allowIdenticalTemplateContentsFromDifferentSources() {
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
        build()
        assertEquals("identical", Files.readString(output("shared.txt")))
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
