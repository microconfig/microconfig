package io.microconfig.core.properties.repository.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.core.properties.repository.ComponentGraph
import io.microconfig.core.properties.repository.ConfigFile
import java.io.File
import java.util.Optional

class EnvProfilesComponentGraph(
    private val delegate: ComponentGraph,
    private val environments: EnvironmentRepository,
) : ComponentGraph {
    override fun getConfigFilesOf(
        component: String,
        environment: String,
        configType: ConfigType,
    ): List<ConfigFile> =
        configFiles(component, environment, configType, emptyList(), mutableMapOf())

    private fun configFiles(
        component: String,
        environment: String,
        type: ConfigType,
        visiting: List<String>,
        completed: MutableMap<String, List<ConfigFile>>,
    ): List<ConfigFile> {
        checkProfileCycle(environment, visiting)
        return completed.getOrPut(environment) {
            val standard = delegate.getConfigFilesOf(component, environment, type)
            val profiles =
                profileFiles(component, environment, type, visiting + environment, completed)
            joinConfigs(standard, profiles, environment)
        }
    }

    private fun checkProfileCycle(environment: String, visiting: List<String>) {
        if (environment in visiting)
            throw EnvironmentException(
                "Cyclic environment profiles: ${(visiting + environment).joinToString(" -> ")}"
            )
    }

    private fun profileFiles(
        component: String,
        environment: String,
        type: ConfigType,
        visiting: List<String>,
        completed: MutableMap<String, List<ConfigFile>>,
    ): List<ConfigFile> =
        environments.getOrCreateByName(environment).profiles.flatMap {
            configFiles(component, it, type, visiting, completed)
        }

    private fun joinConfigs(
        standard: List<ConfigFile>,
        profiles: List<ConfigFile>,
        environment: String,
    ): List<ConfigFile> {
        if (profiles.isEmpty()) return standard
        val inherited =
            profiles
                .filterNot { it.file.name.contains(".$environment.") }
                .map { it.withEnvironment(environment) }
                .distinct()
        return inherited + (standard - inherited.toSet())
    }

    override fun getFolderOf(component: String): Optional<File> = delegate.getFolderOf(component)
}
