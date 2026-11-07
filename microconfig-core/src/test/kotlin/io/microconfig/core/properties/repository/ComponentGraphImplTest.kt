package io.microconfig.core.properties.repository

import io.microconfig.core.Microconfig
import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.configtypes.StandardConfigType.PROCESS
import io.microconfig.core.properties.repository.impl.ComponentGraphImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ComponentGraphImplTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun snapshotDirectoryEntriesAcrossTypesAndEnvironmentsWithoutCachingConfigFiles() {
        val shared = write("components/app/config.yaml")
        val dev = write("components/app/config.dev.yaml")
        val prod = write("components/app/config.prod.yaml")
        val process = write("components/app/runtime.process")
        write("components/app/runtime.dev.process")
        val prodProcess = write("components/app/runtime.prod.process")
        val graph = ComponentGraphImpl.traverseFrom(root.toFile())

        val original = graph.getConfigFilesOf("app", "dev", APPLICATION)
        assertEquals(listOf(shared, dev), original.map { it.file })
        write("components/app/late.yaml")
        write("components/app/late.process")

        val processes = graph.getConfigFilesOf("app", "prod", PROCESS)
        assertEquals(listOf(process, prodProcess), processes.map { it.file })
        processes.forEach {
            assertEquals("process", it.configType)
            assertEquals("prod", it.environment)
        }
        val customType = object : ConfigType by APPLICATION {
            override val typeName = "custom"
        }
        val custom = graph.getConfigFilesOf("app", "prod", customType)
        assertEquals(listOf(shared, prod), custom.map { it.file })
        custom.forEach {
            assertEquals("custom", it.configType)
            assertEquals("prod", it.environment)
        }
        original.forEach {
            assertEquals("app", it.configType)
            assertEquals("dev", it.environment)
        }
        assertNotSame(original.first(), custom.first())
    }

    @Test
    fun cacheEmptyDirectoryListingAndRefreshOnlyInANewGraph() {
        Files.createDirectories(root.resolve("components/app"))
        val graph = ComponentGraphImpl.traverseFrom(root.toFile())
        assertTrue(graph.getConfigFilesOf("app", "dev", APPLICATION).isEmpty())

        val application = write("components/app/config.yaml")
        val process = write("components/app/runtime.process")

        assertTrue(graph.getConfigFilesOf("app", "prod", APPLICATION).isEmpty())
        assertTrue(graph.getConfigFilesOf("app", "dev", PROCESS).isEmpty())
        val fresh = ComponentGraphImpl.traverseFrom(root.toFile())
        assertEquals(listOf(application), fresh.getConfigFilesOf("app", "dev", APPLICATION).map { it.file })
        assertEquals(listOf(process), fresh.getConfigFilesOf("app", "dev", PROCESS).map { it.file })
    }

    @Test
    fun preserveFilePriorityAndAllFoldersWithTheSameComponentName() {
        val firstShared = write("components/first/app/config.yaml")
        val secondShared = write("components/second/app/config.yaml")
        val twenty = write("components/first/app/20.yaml")
        val ten = write("components/second/app/10.yaml")
        val firstMultiEnvironment = write("components/first/app/config.dev.prod.yaml")
        val secondMultiEnvironment = write("components/second/app/z.dev.prod.yaml")
        val firstSpecific = write("components/first/app/a.dev.yaml")
        val secondSpecific = write("components/second/app/z.dev.yaml")
        write("components/first/app/ignored.prod.yaml")
        write("components/second/app/ignored.txt")
        Files.createDirectories(root.resolve("components/first/app/directory.yaml"))
        val graph = ComponentGraphImpl.traverseFrom(root.toFile())
        val primary = graph.getFolderOf("app").orElseThrow()
        val shared = if (primary == firstShared.parentFile) {
            listOf(firstShared, secondShared)
        } else {
            listOf(secondShared, firstShared)
        }

        val files = graph.getConfigFilesOf("app", "dev", APPLICATION)

        assertEquals(
            listOf(ten, twenty) + shared +
                    listOf(firstMultiEnvironment, secondMultiEnvironment, firstSpecific, secondSpecific),
            files.map { it.file },
        )
    }

    @Test
    fun preserveProfileOrderingAndCurrentEnvironmentInThePublicGraph() {
        write("envs/dev.yaml", "profiles: [base, shared]\ngroup:\n  components: [app]\n")
        write("envs/base.yaml", "{}\n")
        write("envs/shared.yaml", "{}\n")
        val shared = write("components/app/settings.yaml")
        val profiles = write("components/app/settings.base.shared.yaml")
        val base = write("components/app/settings.base.yaml")
        val sibling = write("components/app/settings.shared.yaml")
        val environmentAndProfile = write("components/app/settings.dev.base.yaml")
        val environment = write("components/app/settings.dev.yaml")
        val graph = Microconfig.searchConfigsIn(root.toFile()).dependencies().componentGraph()
        val expected = listOf(shared, profiles, base, sibling, environmentAndProfile, environment)
            .map { it.canonicalFile }

        assertEquals(expected, graph.getConfigFilesOf("app", "dev", APPLICATION).map { it.file })
        write("components/app/late.yaml")
        val files = graph.getConfigFilesOf("app", "dev", APPLICATION)

        assertEquals(expected, files.map { it.file })
        files.forEach {
            assertEquals("app", it.configType)
            assertEquals("dev", it.environment)
        }
    }

    private fun write(path: String, content: String = "value: present\n"): File {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
        return file.toFile()
    }
}
