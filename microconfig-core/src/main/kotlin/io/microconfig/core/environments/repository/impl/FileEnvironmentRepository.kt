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
import java.util.Optional

class FileEnvironmentRepository(
    rootDir: File,
    private val fsReader: FsReader,
    private val componentFactory: ComponentFactory,
    private val propertiesFactory: PropertiesFactory,
) : EnvironmentRepository {
    private val envDir = File(rootDir, ENV_DIR)

    init {
        require(envDir.exists()) { "'$ENV_DIR' directory doesn't exist: $envDir" }
    }

    override fun environments(): List<Environment> =
        environmentFiles().map(::parse).filterNot { it.isAbstract }

    private fun environmentFiles(): List<File> =
        walk(envDir.toPath()).use { paths ->
            paths.map { it.toFile() }.filter { it.name.endsWith(".yaml") }.toList()
        }

    private fun parse(file: File): Environment =
        parseDefinition(file, emptyList()).toEnvironment(componentFactory, propertiesFactory)

    private fun parseDefinition(file: File, visiting: List<String>): EnvironmentDefinition {
        val name = file.nameWithoutExtension
        checkIncludeCycle(name, visiting)
        val definition = EnvironmentFile(file).parseUsing(fsReader)
        return definition
            .processIncludeUsing { included ->
                val source = envFileWith(included).orElseThrow { notFoundException(included) }
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

    private fun envFileWith(name: String): Optional<File> {
        val files = environmentFiles().filter { it.nameWithoutExtension == name }
        if (files.size > 1) throw EnvironmentException("Found several env files with name: $name")
        return Optional.ofNullable(files.firstOrNull())
    }

    private fun notFoundException(name: String): EnvironmentException =
        EnvironmentException(
            "Can't find env '$name'. Available env files: ${environmentFiles().map { it.nameWithoutExtension }.toSortedSet()}"
        )

    override fun environmentNames(): Set<String> = environmentNames(emptySet())

    override fun environmentNames(excluded: Set<String>): Set<String> =
        environmentFiles()
            .filterNot { it.nameWithoutExtension in excluded }
            .map(::parse)
            .filterNot { it.isAbstract }
            .map { it.name }
            .toSortedSet()

    override fun getByName(name: String): Environment =
        findEnvWith(name).orElseThrow { notFoundException(name) }

    private fun findEnvWith(name: String): Optional<Environment> = envFileWith(name).map(::parse)

    override fun getOrCreateByName(name: String): Environment =
        findEnvWith(name).orElseGet { fakeEnvironment(name) }

    private fun fakeEnvironment(name: String): Environment =
        EnvironmentImpl(
            null,
            name,
            false,
            0,
            emptyList(),
            emptyList(),
            componentFactory,
            propertiesFactory,
        )

    companion object {
        const val ENV_DIR = "envs"
    }
}
