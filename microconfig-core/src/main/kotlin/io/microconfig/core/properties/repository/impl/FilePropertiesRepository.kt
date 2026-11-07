package io.microconfig.core.properties.repository.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.repository.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FilePropertiesRepository(
    private val componentGraph: ComponentGraph,
    private val environments: EnvironmentRepository,
    private val configIo: ConfigIo,
) : PropertiesRepository {
    private val parsed = ConcurrentHashMap<Triple<File, String, String>, RawConfig>()

    override fun getPropertiesOf(
        originalComponentName: String,
        environment: String,
        configType: ConfigType,
    ): Map<String, Property> =
        OriginalComponent(originalComponentName, environment, configType, linkedSetOf())
            .properties()

    private inner class OriginalComponent(
        private val component: String,
        private val environment: String,
        private val configType: ConfigType,
        private val processedIncludes: MutableSet<Include>,
    ) {
        fun properties(): Map<String, Property> {
            val files = componentGraph.getConfigFilesOf(component, environment, configType)
            if (files.size == 1) return parse(files.single())
            return YamlScalars.indexProperties(files.flatMap { parse(it).values })
        }

        private fun parse(file: ConfigFile): Map<String, Property> =
            parsed.computeIfAbsent(Triple(file.file, file.configType, file.environment)) {
                file.parseUsing(configIo)
            }
                .getBaseAndIncludedProperties(
                    ::resolveInclude,
                    environments.profilesOf(environment),
                    environment,
                )

        private fun resolveInclude(include: Include): Map<String, Property> =
            try {
                if (processedIncludes.add(include))
                    OriginalComponent(
                        include.component,
                        include.environment,
                        configType,
                        processedIncludes,
                    )
                        .properties()
                else emptyMap()
            } catch (error: ComponentNotFoundException) {
                throw error.withParentComponent(component)
            }
    }
}
