package io.microconfig.core.properties.repository.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.properties.repository.ComponentGraph
import io.microconfig.core.properties.repository.ComponentNotFoundException
import io.microconfig.core.properties.repository.ConfigFile
import io.microconfig.utils.FileUtils.walk
import io.microconfig.utils.Logger.warn
import java.io.File
import java.nio.file.Files
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class ComponentGraphImpl
private constructor(private val foldersByComponent: Map<String, List<File>>) : ComponentGraph {
    private val filesByFolder = ConcurrentHashMap<File, List<File>>()

    override fun getConfigFilesOf(
        component: String,
        environment: String,
        configType: ConfigType,
    ): List<ConfigFile> {
        val folders = componentFolders(component)
        return matchingFiles(folders, environment, configType).map {
            ConfigFile(it, configType.typeName, environment)
        }
    }

    private fun componentFolders(component: String): List<File> =
        foldersByComponent[component] ?: throw ComponentNotFoundException(component)

    private fun matchingFiles(
        folders: List<File>,
        environment: String,
        type: ConfigType,
    ): List<File> =
        folders
            .flatMap(::filesIn)
            .filter { file ->
                val extension = file.extension.takeIf(String::isNotEmpty)?.let { ".$it" }.orEmpty()
                file.isFile &&
                    extension in type.sourceExtensions &&
                    isForEnvironment(file, environment)
            }
            .sortedWith(compareBy<File>(::environmentPriority).thenBy { it.name })

    private fun filesIn(folder: File): List<File> =
        filesByFolder.computeIfAbsent(folder) { it.listFiles()?.toList().orEmpty() }

    private fun isForEnvironment(file: File, environment: String): Boolean =
        file.name.count { it == '.' } == 1 || environment.isEmpty() || ".$environment." in file.name

    private fun environmentPriority(file: File): Long {
        val count = file.name.count { it == '.' } - 1
        return if (count == 0) Long.MIN_VALUE else -count.toLong()
    }

    override fun getFolderOf(component: String): Optional<File> {
        val folders = foldersByComponent[component].orEmpty()
        if (folders.size > 1)
            warn(
                "Found ${folders.size} folders with name '$component'. Consider renaming them, otherwise placeholder resolution works incorrectly"
            )
        return Optional.ofNullable(folders.firstOrNull())
    }

    companion object {
        const val COMPONENTS_DIR = "components"

        fun traverseFrom(rootDir: File): ComponentGraph {
            val components = File(rootDir, COMPONENTS_DIR)
            require(components.exists()) { "Root directory must contain 'components' dir" }
            return walk(components.toPath()).use { paths ->
                ComponentGraphImpl(
                    paths
                        .filter(Files::isDirectory)
                        .map { it.toFile() }
                        .toList()
                        .groupBy { it.name }
                )
            }
        }
    }
}
