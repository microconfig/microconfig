package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.Environment
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.environments.impl.EnvironmentImpl
import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.io.FsReader
import io.microconfig.utils.FileUtils.walk
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FileEnvironmentRepository(
    rootDir: File,
    private val fsReader: FsReader,
    private val componentFactory: ComponentFactory,
    private val propertiesFactory: PropertiesFactory,
) : EnvironmentRepository {
    private val envDir = File(rootDir, ENV_DIR)
    private val parsed = ConcurrentHashMap<File, Environment>()
    private val profilesByName = ConcurrentHashMap<String, List<String>>()
    private val files by lazy(::readEnvironmentFiles)

    init {
        require(envDir.exists()) { "'$ENV_DIR' directory doesn't exist: $envDir" }
    }

    override fun environments(): List<Environment> =
        files.map(::parse).filterNot { it.isAbstract }

    private fun readEnvironmentFiles(): List<File> =
        walk(envDir.toPath()).use { paths ->
            paths.map { it.toFile() }.filter { it.name.endsWith(".yaml") }.toList()
        }

    private fun parse(file: File): Environment =
        parsed.computeIfAbsent(file) {
            parseDefinition(it, emptyList()).toEnvironment(componentFactory, propertiesFactory)
        }

    private fun parseDefinition(file: File, visiting: List<String>): EnvironmentDefinition {
        val name = file.nameWithoutExtension
        checkIncludeCycle(name, visiting)
        val definition = EnvironmentFile(file).parseUsing(fsReader)
        return definition
            .processIncludeUsing { included ->
                val source = envFileWith(included) ?: throw notFoundException(included)
                parseDefinition(source, visiting + name)
            }
            .checkComponentNamesAreUnique()
    }

    private fun checkIncludeCycle(name: String, visiting: List<String>) {
        if (name in visiting)
            throw EnvironmentException(
                "Cyclic environment includes: ${(visiting + name).joinToString(" -> ")}"
            )
    }

    private fun envFileWith(name: String): File? {
        val matching = files.filter { it.nameWithoutExtension == name }
        if (matching.size > 1) throw EnvironmentException("Found several env files with name: $name")
        return matching.firstOrNull()
    }

    private fun notFoundException(name: String): EnvironmentException =
        EnvironmentException(
            "Can't find env '$name'. Available env files: ${files.map { it.nameWithoutExtension }.toSortedSet()}"
        )

    override fun environmentNames(): Set<String> = environmentNames(emptySet())

    override fun environmentNames(excluded: Set<String>): Set<String> =
        files
            .filterNot { it.nameWithoutExtension in excluded }
            .map(::parse)
            .filterNot { it.isAbstract }
            .map { it.name }
            .toSortedSet()

    override fun profilesOf(name: String): List<String> =
        profilesByName.computeIfAbsent(name) { super<EnvironmentRepository>.profilesOf(it) }.toList()

    override fun getByName(name: String): Environment =
        findEnvWith(name) ?: throw notFoundException(name)

    private fun findEnvWith(name: String): Environment? = envFileWith(name)?.let(::parse)

    override fun getOrCreateByName(name: String): Environment =
        findEnvWith(name) ?: fakeEnvironment(name)

    private fun fakeEnvironment(name: String): Environment =
        EnvironmentImpl(
            null,
            name,
            false,
            emptyList(),
            emptyList(),
            componentFactory,
            propertiesFactory,
        )

    companion object {
        const val ENV_DIR = "envs"
    }
}
